/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.operation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.DatabaseTestCase;
import org.mongodb.Document;
import org.mongodb.MongoException;
import org.mongodb.MongoNamespace;
import org.mongodb.codecs.DocumentCodec;
import org.mongodb.connection.Cluster;
import org.mongodb.connection.ClusterSettings;
import org.mongodb.connection.ConnectionPoolSettings;
import org.mongodb.connection.DefaultClusterFactory;
import org.mongodb.connection.ServerSettings;
import org.mongodb.connection.SocketSettings;
import org.mongodb.connection.SocketStreamFactory;
import org.mongodb.session.ClusterSession;
import org.mongodb.session.Session;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mongodb.Fixture.getPrimary;
import static org.mongodb.Fixture.getSSLSettings;
import static org.mongodb.Fixture.getSession;
import static org.mongodb.Fixture.isAuthenticated;
import static org.mongodb.Fixture.getExecutor;
import static org.mongodb.MongoCredential.createMongoCRCredential;
import static org.mongodb.WriteConcern.ACKNOWLEDGED;

// This test is here because the assertion is conditional on auth being enabled, and there"s no way to do that in Spock
@SuppressWarnings("unchecked")
public class UserOperationTest extends DatabaseTestCase {
    private User readOnlyUser;

    @Before
    public void setUp() {
        super.setUp();
        readOnlyUser = new User(createMongoCRCredential("jeff", getDatabaseName(), "123".toCharArray()), true);
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void readOnlyUserShouldNotBeAbleToWrite() throws Exception {
        assumeTrue(isAuthenticated());

        // given:
        new CreateUserOperation(readOnlyUser).execute(getSession());
        Cluster cluster = createCluster(readOnlyUser);
        Session session = new ClusterSession(cluster, getExecutor());

        // when:
        try {
            new InsertOperation<Document>(collection.getNamespace(), true, ACKNOWLEDGED,
                                          asList(new InsertRequest<Document>(new Document())),
                                          new DocumentCodec()
            ).execute(session);
            fail("should have thrown");
        } catch (MongoException e) {
            // all good
        } finally {
            // cleanup:
            new DropUserOperation(getDatabaseName(), readOnlyUser.getCredential().getUserName()
            ).execute(getSession());
            cluster.close();
        }
    }

    @Test
    public void readWriteAdminUserShouldBeAbleToWriteToADifferentDatabase() throws InterruptedException {
        assumeTrue(isAuthenticated());

        // given
        User adminUser = new User(createMongoCRCredential("jeff-rw-admin", "admin", "123".toCharArray()), false);
        new CreateUserOperation(adminUser).execute(getSession());

        Cluster cluster = createCluster(adminUser);
        Session session = new ClusterSession(cluster, getExecutor());

        try {
            // when
            new InsertOperation<Document>(new MongoNamespace(getDatabaseName(), getCollectionName()),
                                          true, ACKNOWLEDGED,
                                          asList(new InsertRequest<Document>(new Document())),
                                          new DocumentCodec()
            ).execute(session);
            // then
            assertEquals(1L, (long) new CountOperation(new MongoNamespace(getDatabaseName(), getCollectionName()), new Find(),
                                                       new DocumentCodec()
            )
                                    .execute(session));
        } finally {
            // cleanup
            new DropUserOperation("admin", adminUser.getCredential().getUserName()).execute(getSession());
            cluster.close();
        }
    }

    @Test
    public void readOnlyAdminUserShouldNotBeAbleToWriteToADatabase() throws InterruptedException {
        assumeTrue(isAuthenticated());
        // given
        User adminUser = new User(createMongoCRCredential("jeff-ro-admin", "admin", "123".toCharArray()), true);
        new CreateUserOperation(adminUser).execute(getSession());

        Cluster cluster = createCluster(adminUser);
        Session session = new ClusterSession(cluster, getExecutor());

        try {
            // when
            new InsertOperation<Document>(new MongoNamespace(getDatabaseName(), getCollectionName()), true, ACKNOWLEDGED,
                                          asList(new InsertRequest<Document>(new Document())),
                                          new DocumentCodec()).execute(session);
            fail("Should have thrown");
        } catch (MongoException e) {
            // all good
        }
        finally {
            // cleanup
            new DropUserOperation("admin", adminUser.getCredential().getUserName()).execute(getSession());
            cluster.close();
        }
    }

    @Test
    public void readOnlyAdminUserShouldBeAbleToReadFromADifferentDatabase() throws InterruptedException {
        assumeTrue(isAuthenticated());

        // given
        User adminUser = new User(createMongoCRCredential("jeff-ro-admin", "admin", "123".toCharArray()), true);
        new CreateUserOperation(adminUser).execute(getSession());

        Cluster cluster = createCluster(adminUser);
        Session session = new ClusterSession(cluster, getExecutor());

        try {
            // when
            long result = new CountOperation(new MongoNamespace(getDatabaseName(), getCollectionName()),
                                             new Find(),
                                             new DocumentCodec()
            ).execute(session);
            // then
            assertEquals(0, result);
        } finally {
            // cleanup
            new DropUserOperation("admin", adminUser.getCredential().getUserName()).execute(getSession());
            cluster.close();
        }
    }


    private Cluster createCluster(final User user) throws InterruptedException {
        return new DefaultClusterFactory().create(ClusterSettings.builder().hosts(asList(getPrimary())).build(),
                                                  ServerSettings.builder().build(),
                                                  ConnectionPoolSettings.builder().maxSize(1).maxWaitQueueSize(1).build(),
                                                  new SocketStreamFactory(SocketSettings.builder().build(), getSSLSettings()),
                                                  new SocketStreamFactory(SocketSettings.builder().build(), getSSLSettings()),
                                                  asList(user.getCredential()),
                                                  null, null, null);
    }

}