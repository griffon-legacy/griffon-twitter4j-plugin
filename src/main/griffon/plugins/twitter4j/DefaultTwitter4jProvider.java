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

package griffon.plugins.twitter4j;

import twitter4j.Twitter;

import java.util.Map;

/**
 * @author Andres Almiray
 */
public class DefaultTwitter4jProvider extends AbstractTwitter4jProvider {
    private static final DefaultTwitter4jProvider INSTANCE;

    static {
        INSTANCE = new DefaultTwitter4jProvider();
    }

    public static DefaultTwitter4jProvider getInstance() {
        return INSTANCE;
    }

    private DefaultTwitter4jProvider() {}

    @Override
    protected Twitter getTwitter(Map<String, Object> params) {
        return TwitterHolder.getInstance().fetchTwitter(params);
    }
}