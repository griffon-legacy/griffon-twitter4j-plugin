/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.plugins.twitter4j

import twitter4j.Twitter

import java.util.concurrent.ConcurrentHashMap

/**
 * @author Andres Almiray
 */
class TwitterHolder {
    private static final TwitterHolder INSTANCE

    static {
        INSTANCE = new TwitterHolder()
    }

    static TwitterHolder getInstance() {
        INSTANCE
    }

    private TwitterHolder() {}

    private final Map<String, Twitter> CLIENTS = new ConcurrentHashMap<String, Twitter>()

    String[] getTwitterIds() {
        List<String> ids = []
        ids.addAll(CLIENTS.keySet())
        ids.toArray(new String[ids.size()])
    }

    Twitter getTwitter(String id) {
        CLIENTS[id]
    }

    void setTwitter(String id, Twitter client) {
        CLIENTS[id] = client
    }

    // ======================================================

    Twitter fetchTwitter(Map<String, Object> params) {
        Twitter client = CLIENTS[(params.id).toString()]
        if (client == null) {
            String id = params.id ? params.remove('id').toString() : '<EMPTY>'
            client = TwitterConnector.instance.createClient(params)
            if (id != '<EMPTY>') CLIENTS[id] = client
        }
        client
    }
}