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

import griffon.util.CallableWithArgs;
import groovy.lang.Closure;

import java.util.Map;

/**
 * @author Andres Almiray
 */
public class Twitter4jContributionAdapter implements Twitter4jContributionHandler {
    private Twitter4jProvider provider = DefaultTwitter4jProvider.getInstance();

    public void setTwitter4jProvider(Twitter4jProvider provider) {
        this.provider = provider != null ? provider : DefaultTwitter4jProvider.getInstance();
    }

    public Twitter4jProvider getTwitter4jProvider() {
        return provider;
    }

    public <R> R withTwitter(Map<String, Object> params, Closure<R> closure) {
        return provider.withTwitter(params, closure);
    }

    public <R> R withTwitter(Map<String, Object> params, CallableWithArgs<R> callable) {
        return provider.withTwitter(params, callable);
    }
}