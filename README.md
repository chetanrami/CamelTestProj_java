CamelTest Project
=================

This Maven project represents a fairly common integration: copying files from one source via SFTP, transforming the data,
and copying the result to a target system via SFTP.

This project uses Groovy, although you're free to write your solution in Java if you prefer.

This project's main route is already ironed out, you can review it in the cameltestprojectRoutes.groovy file.
The bundle receives an XML from the remote system, converts it using StAX and Bindy into a CSV file, then copies the result to a target system.

Write unit tests for this project in order to specify two areas of concern: the critical path of the project and some error handling.
See [Camel's documentation on testing](https://camel.apache.org/testing) for some direction in testing Camel (using Blueprint).
We provide a base class `ArgoCamelTestSupport` which declares three endpoints and configures the project to test Blueprint based Camel projects.

`CriticalPathTest` is the class which you'll write two (or more) methods (there are TODO comments in the relevant sections).
The goal of this class is to ensure that the critical path of the route works.

`ErrorHandlerTest` is the class which you'll write two (or more) methods (there are TODO comments in the relevant sections).
The goal of this class is to ensure that when an error occurs (e.g. an exception is thrown) we correctly route a message to an 
error endpoint for later handling. In production, we often use an ActiveMQ queue to store our error inputs for later handling.

