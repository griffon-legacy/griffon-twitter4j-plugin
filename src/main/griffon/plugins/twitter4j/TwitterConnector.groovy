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
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import griffon.util.ApplicationHolder

import static griffon.util.GriffonNameUtils.getSetterName

/**
 * @author Andres Almiray
 */
@Singleton
class TwitterConnector {
    public Twitter createClient(Map params) {
        ConfigObject config = new ConfigObject()
        ConfigObject twitterConfig = ApplicationHolder.application.config.twitter4j
        if (twitterConfig) config.merge(twitterConfig)
        if (params) config.putAll(params)

        ConfigurationBuilder builder = new ConfigurationBuilder()
        for (entry in config) {
            if (entry.key == 'class' || entry.key == 'metaClass') continue
            try {
                String setter = getSetterName(entry.key)
                builder."$setter"(entry.value)
            } catch (MissingPropertyException x) {
                // ignore ?
            }catch (MissingMethodException x) {
                // ignore ?
            }
        }

        TwitterFactory twitterFactory = new TwitterFactory(builder.build())
        twitterFactory.instance
    }
}