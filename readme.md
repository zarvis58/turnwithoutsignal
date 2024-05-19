# TurnWithoutSignal

This project is a Java application that allows two users to exchange messages over a network using the DatagramSocket API.

## Getting Started

These instructions will get  a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

- Java 8 or higher
- Maven

### Instruction

1. Clone the repository
2. Navigate to the project directory
3. Run `mvn install` to install the necessary dependencies

4. Run `mvn exec:java` to start the User1 
5. Edit the `pom.xml` file to start User2 by changing the main class to `com.safeai.User2` as shown below:

 ```bash
                <configuration>
                    <mainClass>com.safeai.User2</mainClass>
                </configuration>
```
6. Run again this in a new terminal `mvn exec:java` to start User2
7. Wait until 'Enter Message' is displayed on the terminal
8. Now  can start sending messages between User1 and User2
                




Both users can then exchange messages.

## Built With

- [Java](https://www.java.com) - The programming language used
- [Maven](https://maven.apache.org/) - Dependency Management

