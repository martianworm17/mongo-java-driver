/*
 * Copyright 2008-present MongoDB, Inc.
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

package com.mongodb.session;

import org.bson.BsonDocument;

/**
 * A MongoDB server session.
 *
 * @mongodb.server.release 3.6
 * @since 3.6
 */
public interface ServerSession {

    /**
     * @return the server session identifier
     */
    BsonDocument getIdentifier();

    /**
     * Gets the current transaction number.
     *
     * @return the current transaction number
     * @since 3.8
     */
    long getTransactionNumber();

    /**
     * Gets the current statement id.  The statement id is reset to 0 at the start of transaction
     *
     * @return the statement identifier
     * @since 3.8
     * @mongodb.server.release 4.0
     */
    int getStatementId();

    /**
     * Advance the statement identifier by the given increment.
     *
     * @param increment the increment, which must be &gt;= 1
     * @return the statement identifier prior to advancement
     * @since 3.8
     * @mongodb.server.release 4.0
     */
    int advanceStatementId(int increment);

    /**
     * Return the next available transaction number.
     *
     * @return the next transaction number
     */
    long advanceTransactionNumber();

    /**
     * Whether the server session is closed.
     *
     * @return true if the session has been closed
     */
    boolean isClosed();
}