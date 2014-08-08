Rhizomer
========
  
Rhizomer is a web application that facilitates publishing, querying, browsing, editing and interacting with semantic data.
  
More details are available from [http://rhizomik.net/rhizomer/](http://rhizomik.net/rhizomer/)
  
Download and Install
--------------------

The application plus the required libraries is available as a [WAR file](http://rhizomik.net/html/rhizomer/install/rhizomer.war) and the source code from the [GitHub](https://github.com/rhizomik/rhizomer).

The WAR file should work in any servlet/jsp container. However, It has been just tested with Tomcat and Glassfish.

If placed in the Tomcat's webapps folder, it should be automatically expanded and installed as and application available from /rhizomer (typically http://localhost:8080/rhizomer).

If installed elsewhere, you should just adjust the path to the users.xml file in the META-INF/context.xml file, currently "webapps/rhizomer/META-INF/users.xml". There is just one default user named "rhizomer" with password "rhizomer".

Basic Configuration
-------------------

If you go now to the installed Rhizomer homepage (typically http://localhost:8080/rhizomer), you will see the NASA Apollo dataset being published, just like at [http://rhizomik.net/apollo/](http://rhizomik.net/apollo/). The dataset being published is locally stored as a file and loaded at start time from metadata/nasa-apollo.rdf

You can change it or configure a different file to load RDF from changing the WEB-INF/web.xml file. The Rhizomer servlet parameters specify if the RDF metadata is loaded/stored from a file (by defining folder and file name) or from a database (currently Virtuoso, OWLIM or any relational databased supported by Jena).

Feedback
--------

Please, if you have any comment or issue, send them to us using the [issues tracker](https://github.com/rhizomik/rhizomer/issues).