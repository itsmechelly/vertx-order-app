
# Vert.x Order Application

### 🤔 What is the purpose of this application?
This Project represents a small Microservice App using Eclipse Vert.x and Hazelcast In-Memory Data Grid.
### 💬 What is Eclipse Vert.x?
Eclipse Vert.x is a tool-kit for building reactive asynchronous applications on the JVM.
Reactive applications are both scalable as workloads grow, and resilient when failures arise.
### 💬 How it works?
The project is a Maven project, written by Java language and includes 2 modules that will be communicate with Vert.x Event Bus, the two modules are:
 - RestVerticle module
 - OrderVerticle module

# Extra Details

### 👉 Communication Between the Microservice Modules
The communication between the two verticles will be by using [Vert.x Event Bus](https://vertx.io/).
### 👉 Cluster Manager
The Application will be running in cluster mode by using [Hazelcast In-Memory Data Grid (IMDG)Open Source](https://hazelcast.com/).
### 👉 Docker
The maven package will be generating a docker-compose YAML that will contain two containers for the 2 verticles.

# Application Architecture

## RestVerticle module:
🛠 This java module will be a Vert.x verticle and will contain 2 classes:<br/>
### Main class:
`main(String[] args)` – To run the application in a cluster mode, the main class will use Vert.x implementation of Hazelcast as a cluster manager <br/><br/>
`getAddress()` – This method will use the NetworkInterface to locate and filter the IP addresses.<br/>
The relevant IP address will be sent back to the main method.<br/>
### RestVerticle class:
🛠 First, we will start by creating HTTP Server and Router:<br/><br/>
`start(Promise<Void> startPromise)` – This method starts an HTTP server.
The method create a Vert.x HTTP server and then handle requests using the router generated from the createRouter() method (listening to port 8080).<br/><br/>
`createRouter()` – This method creates a Vert.x Web Router (the object used to route HTTP requests to specific request handlers). The method will return Vert.x Web Router.<br/><br/>
🛠 Then, this class will expose a REST API (using Vert.x) with 5 REST methods:<br/><br/>
`GET: greetingHandler(RoutingContext context)` – This method greeting the user at the main endpoint.<br/><br/>
`POST: loginHandler(RoutingContext context)` – This method will use the RoutingContext interface to get the username and password from the user, and check if the user can be logged in (username and password will be saved in local JSON file). In the background the module should open a session for each user that logged in.<br/><br/>
`POST: logoutHandler(RoutingContext context)` – This method will be used to log out from the user session, his session will be destroyed.<br/><br/>
`POST: addOrderHandler(RoutingContext context)` – This method will add an order to the user.<br/>
By using Vert.x Event Bus, the order will be sent to the OrderVerticle module.<br/><br/>
`GET: getOrdersHandler(RoutingContext context)` – This method will return all user orders.<br/>
The request to get the data will be sent by Vert.x Event Bus to the OrderVerticle module.<br/><br/>
🛠 Extra methods used in this class to support those REST methods:<br/><br/>
`sessionAuth(RoutingContext context)` – Helper method to check if users session is permitted. <br/><br/>
`contextResponse(RoutingContext context, String errorValue, String loginValue, Integer httpStatus)` – Helper method to print error values in case one of the endpoints collapse, or get runtime error.<br/>
This method used in other methods exist in this java class, I added it for clean code. 😊

## OrderVerticle module:
🛠 This java module will be a vert.x verticle and will contain 2 classes:
### Main class:
`main(String[] args)` – To run the application in a cluster mode, the main class will use Vert.x implementation of Hazelcast as a cluster manager. This method will generate the hazelcast configuration and set the destination address.<br/><br/>
`getAddress()` – This method will use the NetworkInterface to locate and filter the IP addresses.<br/>
The relevant IP address will be sent back to the main method.
### OrderVerticle class:
🛠 First, by using Vert.x Event Bus, we will manage requests that received:<br/><br/>
`start(Promise<Void> promise)` – This method use Verte.x Event Bus to manage requests received from the RestVertical module. The Event Bus will direct each request to the relevant method.<br/><br/>
`addOrder(Message<Object> message, String orderId, String orderName, String orderDate)` – This method add new orders to the user existing orders. All the data will be saved in a local JSON file and include: orderID, orderName and orderDate. The response will be sent to the OrderVerticle module.<br/><br/>
`getOrders(Message<Object> message)` – This method will return all user orders.
The response will be sent to the OrderVerticle module.<br/><br/>
🛠 Extra method used in this class to support other methods:<br/><br/>
`messageResponse(Message<Object> message, String errorValue, String insertValue)` –  Helper method to print error values in case one of the endpoints collapse, or get runtime error.<br/>
This method used in other methods exist in this java class, I added it for clean code. 😊


# Endpoints
👉 To run this Microservice properly, you should first run RestVerticle Module, and then the OrderVerticle Module.
#### GET: greetingHandler(RoutingContext context)
```http
  	http://localhost:8080
```
| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `context` | `RoutingContext` | **Required**. The user context |

![image](https://user-images.githubusercontent.com/60425986/229527473-11857d22-231e-4779-8919-4d91a58970a6.png)
#### POST: loginHandler(RoutingContext context)
```http
  	http://localhost:8080/login
```
| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `context` | `RoutingContext` | **Required**. The user context |

![image](https://user-images.githubusercontent.com/60425986/229525638-a08446d6-fca6-4ad4-a433-b209cd5420b5.png)
#### POST: logoutHandler(RoutingContext context)
```http
  	http://localhost:8080/logout
```
| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `context` | `RoutingContext` | **Required**. The user context |

![image](https://user-images.githubusercontent.com/60425986/229525701-df045808-6012-426b-929f-8c981372d0d6.png)

#### POST: addOrderHandler(RoutingContext context)
```http
  	http://localhost:8080/add-order
```
| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `context` | `RoutingContext` | **Required**. The user context |

![image](https://user-images.githubusercontent.com/60425986/229525766-db8206d2-34cb-46b1-bc70-5f6d34896af9.png)
#### GET: getOrdersHandler(RoutingContext context)
```http
  	http://localhost:8080/get-orders
```
| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `context` | `RoutingContext` | **Required**. The user context |

![image](https://user-images.githubusercontent.com/60425986/229525797-a11eb97a-6b43-402b-a921-ec1fc27fde02.png)

<br/><br/>
Thanks for reading,
<br/>
Chelly 👩🏻‍💻
