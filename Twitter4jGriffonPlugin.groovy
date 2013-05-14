/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Andres Almiray
 */
class Twitter4jGriffonPlugin {
    // the plugin version
    String version = '1.0.1'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '1.3.0 > *'
    // the other plugins this plugin depends on
    Map dependsOn = [lombok: '0.5.0']
    // resources that are included in plugin packaging
    List pluginIncludes = []
    // the plugin license
    String license = 'Apache Software License 2.0'
    // Toolkit compatibility. No value means compatible with all
    // Valid values are: swing, javafx, swt, pivot, gtk
    List toolkits = []
    // Platform compatibility. No value means compatible with all
    // Valid values are:
    // linux, linux64, windows, windows64, macosx, macosx64, solaris
    List platforms = []
    // URL where documentation can be found
    String documentation = ''
    // URL where source can be found
    String source = 'https://github.com/griffon/griffon-twitter4j-plugin'

    List authors = [
        [
            name: 'Andres Almiray',
            email: 'aalmiray@yahoo.com'
        ],
        [
            name: 'Mario Garcia',
            email: 'mario.garcia@gmail.com'
        ]
    ]
    String title = 'Twitter4j integration'

    String description = '''
Integrate your Griffon application with the Twitter service via [Twitter4j][1].

Usage
-----

The plugin will inject the following dynamic methods:

 * `<R> R withTwitter(Map<String, Object> params, Closure<R> closure)`
 * `<R> R withTwitter(Map<String, Object> params, CallableWithArgs<R> callable)`

Where params may contain any of the properties of [twitter4j.conf.ConfigurationBuilder][2].

All dynamic methods will create a new client when invoked unless you define an
`id:` attribute. When this attribute is supplied the client will be stored in
a cache managed by `griffon.plugins.twitter4j.TwitterHolder`.

These methods are also accessible to any component through the singleton
`griffon.plugins.twitter4j.DefaultTwitter4jProvider`. You can inject these methods to
non-artifacts via metaclasses. Simply grab hold of a particular metaclass and
call `griffon.plugins.twitter4j.Twitter4jEnhancer.enhance(metaClassInstance)`.

Here's an brief example that shows how these methods may be used

__Print Statuses from Home Timeline__

    withTwitter(id: 'someClientId') { twitter ->
        for (status in twitter.homeTimeline) {
            println "@${status.user.name} => ${status.text}"
        }
    }

Configuration
-------------

### Twitter4jAware AST Transformation

The preferred way to mark a class for method injection is by annotating it with
`@griffon.plugins.twitter4j.Twitter4jAware`. This transformation injects the
`griffon.plugins.twitter4j.Twitter4jContributionHandler` interface and default behavior
that fulfills the contract.

### Dynamic Method Injection

Dynamic methods will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.twitter4j.injectInto = ['controller', 'service']

Dynamic method injection will be skipped for classes implementing
`griffon.plugins.twitter4j.Twitter4jContributionHandler`.

### Properties

All properties of [twitter4j.conf.ConfigurationBuilder][2] may also be specified
in the application's configuration file (`Config.groovy`), using `twitter4j` as
a prefix, for example

    twitter4j {
        OAuthConsumerKey       = '********'
        OAuthConsumerSecret    = '********'
        OAuthAccessToken       = '********'
        OAuthAccessTokenSecret = '********'
    }

Testing
-------

Dynamic methods will not be automatically injected during unit testing, because
addons are simply not initialized for this kind of tests. However you can use
`Twitter4jEnhancer.enhance(metaClassInstance, twitter4jProviderInstance)` where
`twitter4jProviderInstance` is of type `griffon.plugins.twitter4j.Twitter4jProvider`.
The contract for this interface looks like this

    public interface Twitter4jProvider {
        <R> R withTwitter(Map<String, Object> params, Closure<R> closure);
        <R> R withTwitter(Map<String, Object> params, CallableWithArgs<R> callable);
    }

It's up to you define how these methods need to be implemented for your tests.
For example, here's an implementation that never fails regardless of the
arguments it receives

    class MyTwitter4jProvider implements Twitter4jProvider {
        public <R> R withTwitter(Map<String, Object> params, Closure<R> closure) { null }
        public <R> R withTwitter(Map<String, Object> params, CallableWithArgs<R> callable) { null }
    }
    
This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            Twitter4jEnhancer.enhance(service.metaClass, new MyTwitter4jProvider())
            // exercise service methods
        }
    }

On the other hand, if the service is annotated with `@Twitter4jAware` then usage
of `Twitter4jEnhancer` should be avoided at all costs. Simply set
`twitter4jProviderInstance` on the service instance directly, like so, first the
service definition

    @griffon.plugins.twitter4j.Twitter4jAware
    class MyService {
        def serviceMethod() { ... }
    }

Next is the test

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            service.twitter4jProvider = new MyTwitter4jProvider()
            // exercise service methods
        }
    }

Tool Support
------------

### DSL Descriptors

This plugin provides DSL descriptors for Intellij IDEA and Eclipse (provided
you have the Groovy Eclipse plugin installed). These descriptors are found
inside the `griffon-twitter4j-compile-x.y.z.jar`, with locations

 * dsdl/twitter4j.dsld
 * gdsl/twitter4j.gdsl

### Lombok Support

Rewriting Java AST in a similar fashion to Groovy AST transformations is
possible thanks to the [lombok][3] plugin.

#### JavaC

Support for this compiler is provided out-of-the-box by the command line tools.
There's no additional configuration required.

#### Eclipse

Follow the steps found in the [Lombok][3] plugin for setting up Eclipse up to
number 5.

 6. Go to the path where the `lombok.jar` was copied. This path is either found
    inside the Eclipse installation directory or in your local settings. Copy
    the following file from the project's working directory

         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/twitter4j-<version>/dist/griffon-twitter4j-compile-<version>.jar .

 6. Edit the launch script for Eclipse and tweak the boothclasspath entry so
    that includes the file you just copied

        -Xbootclasspath/a:lombok.jar:lombok-pg-<version>.jar:griffon-lombok-compile-<version>.jar:griffon-twitter4j-compile-<version>.jar

 7. Launch Eclipse once more. Eclipse should be able to provide content assist
    for Java classes annotated with `@Twitter4jAware`.

#### NetBeans

Follow the instructions found in [Annotation Processors Support in the NetBeans
IDE, Part I: Using Project Lombok][4]. You may need to specify
`lombok.core.AnnotationProcessor` in the list of Annotation Processors.

NetBeans should be able to provide code suggestions on Java classes annotated
with `@Twitter4jAware`.

#### Intellij IDEA

Follow the steps found in the [Lombok][3] plugin for setting up Intellij IDEA
up to number 5.

 6. Copy `griffon-twitter4j-compile-<version>.jar` to the `lib` directory

         $ pwd
           $USER_HOME/Library/Application Support/IntelliJIdea11/lombok-plugin
         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/twitter4j-<version>/dist/griffon-twitter4j-compile-<version>.jar lib

 7. Launch IntelliJ IDEA once more. Code completion should work now for Java
    classes annotated with `@Twitter4jAware`.


[1]: http://twitter4j.org/en/index.html
[2]: http://twitter4j.org/javadoc/twitter4j/conf/ConfigurationBuilder.html
[3]: /plugin/lombok
[4]: http://netbeans.org/kb/docs/java/annotations-lombok.html
'''
}
