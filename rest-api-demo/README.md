# 概述
本教程将演示如何用Spring Boot开发一个RESTful风格的应用。  
本示例基于实际业务需求，开发一个Account管理系统（这是一个很常见、很典型的需求）。
    
# 开始

- 使用你熟悉的开发工具（本教程中使用的是IntelliJ IDEA），创建项目，添加Spring Initializr模块
- 添加maven依赖
    ```
    ...
    <!-- 1 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- 2 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.hibernate</groupId>
        <artifactId>hibernate-core</artifactId>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>
    ...
    ```
    1. 这是一个web应用，加入web依赖
    2. 我们将使用mysql来存储账户信息，并且使用jpa做数据访问
- 定义实体类
    ```
    @Entity
    @Table(name = "t_account")
    public class Account { // 1
    
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private long id; // 2
    
        @Column(unique = true, nullable = false, length = 20)
        private String username; // 3
    
        @JsonIgnore
        private String password;
    
        private String email;
        private String firstName;
        private String lastName;
        private Integer age;
        private Byte gender;
    
        ... // 4
    ```
    1. 定义Account实体类，用@Entity标记，用@Table指定对应的数据库表名称为t_account；
    2. 定义id字段，并设置为自动增长；
    3. 设置username字段为唯一、非空，这是用户的唯一标识。注意这里需要设置长度，默认长度为255可能会导致不能创建索引，导致unique无效，当然这与mysql的存储引擎也有关系，这里不讨论；
    4. 构造器、getter、setter、toString()等内容
- 实体类操作接口
    ```
    public interface AccountRepository extends CrudRepository<Account, Long> { // 1
        Account findByUsername(String username); // 2
    }
    ```
    1. 定义AccountRepository接口，这个接口不需要实现，只需继承CrudRepository，在其他代码中即可直使用，spring-jpa会自动实例化该接口的对象，并注入到需要的地方；
    2. 我们扩展一个根据username查找Account的方法，这里需要按jpa规则来定义接口方法名称
- 编写控制器
    ```
    @RestController
    @RequestMapping("/api/accounts")
    public class AccountController { // 1
    
    
        @Autowired
        private AccountRepository accountRepository; // 2
    
    
        @RequestMapping(method = RequestMethod.POST, value = "/add")
        public Account add(@RequestBody Account account){ // 3
    
            if(accountRepository.findByUsername(account.getUsername()) != null){
                throw new IllegalArgumentException("用户已经存在！");
            }// 4
    
    
            account.setId(0); // 5
            accountRepository.save(account); // 6
    
            return account; // 7
        }
    
        @RequestMapping(method = RequestMethod.GET, value = "/{username}")
        public Account account(@PathVariable String username) { // 8
    
            Account account = accountRepository.findByUsername(username);
            if(account == null){
                throw new IllegalArgumentException("用户不存在！");
            }
    
            return account;
        }
    
    }
    ```
    1. 定义控制器，用@RestController来标识，同时设定Class级别的访问路径为"/api/accounts"；
    2. 自动注入之前定义好的AccountRepository，用户Account操作；
    3. 定义添加用户方法，访问路径为"/api/accounts/add"，访问方法为POST请求，接收JSON数据、同时返回JSON数据。我们无需指定返回内容格式，spring-mvc框架会自动将前端传入的JSON转为实体对象，并把返回的实体对象转为JSON字符串输出给前端；
    4. 如果用户已经存在，则我们抛出异常，spring-mvc会将异常信息反馈给前端；
    5. 设置accuont的id为0，因为我们之前定义id是自动增长，这里为了防止前端给id赋值；
    6. 保存账户；
    7. 返回账户；
    8. 同样方式定义一个获取用户信息的方法，使用@PathVariable来获取URL中定义的变量，例如请求地址为"/api/accounts/zhangsan"，则这里username的值为"zhangsan"

# 测试
- 配置数据源
    ```
    # 1
    spring.datasource.url=jdbc:mysql://localhost:3306/test
    spring.datasource.username=root
    spring.datasource.password=
    spring.datasource.driver-class-name=com.mysql.jdbc.Driver
    spring.jpa.database=MYSQL
    
    # 2
    spring.jpa.show-sql=true
    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.hibernate.dialect=org.hibernate.dialect.MySQL5Dialect
    spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL5Dialect
    ```
    1. 数据源相关配置项，根据实际情况修改此配置项
    2. hibernate、jpa配置，用spring.jpa.hibernate.ddl-auto=update来配置自动执行DDL操作，且为更新操作，即系统启动时自动根据实体类属性更新数据库表结构
- 创建数据库  

    ```
    略
    ```
    一般安装完MySQL自带test库
- 启动应用  
    
    查看数据，发现已经自动建立了t_account数据表  
    
- 添加Account
    ```
    curl -v -X POST -H "Content-Type: application/json" http://localhost:8010/api/accounts/add -d '{"username": "zhangsan", "password": "1", "email": "zhangsan@example.com", "firstName": "zhang", "lastName": "san"}'
    
    Note: Unnecessary use of -X or --request, POST is already inferred.
    *   Trying ::1...
    * Connected to localhost (::1) port 8010 (#0)
    > POST /api/accounts/add HTTP/1.1
    > Host: localhost:8010
    > User-Agent: curl/7.49.1
    > Accept: */*
    > Content-Type: application/json
    > Content-Length: 115
    >
    * upload completely sent off: 115 out of 115 bytes
    < HTTP/1.1 200
    < Content-Type: application/json;charset=UTF-8
    < Transfer-Encoding: chunked
    < Date: Sun, 20 Nov 2016 15:29:15 GMT
    <
    * Connection #0 to host localhost left intact
    {"id":1,"username":"zhangsan","email":"zhangsan@example.com","firstName":"zhang","lastName":"san","age":null,"gender":null}
    ```
    工作正常
    
- 查看Account
    ```
    curl -v -X GET http://localhost:8010/api/accounts/zhangsan
    
    Note: Unnecessary use of -X or --request, GET is already inferred.
    *   Trying ::1...
    * Connected to localhost (::1) port 8010 (#0)
    > GET /api/accounts/zhangsan HTTP/1.1
    > Host: localhost:8010
    > User-Agent: curl/7.49.1
    > Accept: */*
    >
    < HTTP/1.1 200
    < Content-Type: application/json;charset=UTF-8
    < Transfer-Encoding: chunked
    < Date: Sun, 20 Nov 2016 15:32:13 GMT
    <
    * Connection #0 to host localhost left intact
    {"id":1,"username":"zhangsan","email":"zhangsan@example.com","firstName":"zhang","lastName":"san","age":null,"gender":null}
    ```
    工作正常  
    
# 进阶
- 错误处理  

    尝试添加一个已经存在的用户，让后台抛出异常
    ```
    curl -v -X POST -H "Content-Type: application/json" http://localhost:8010/api/accounts/add -d '{"username": "zhangsan", "password": "1", "email": "zhangsan@example.com", "firstName": "zhang", "lastName": "san"}'
    
    Note: Unnecessary use of -X or --request, POST is already inferred.
    *   Trying ::1...
    * Connected to localhost (::1) port 8010 (#0)
    > POST /api/accounts/add HTTP/1.1
    > Host: localhost:8010
    > User-Agent: curl/7.49.1
    > Accept: */*
    > Content-Type: application/json
    > Content-Length: 115
    >
    * upload completely sent off: 115 out of 115 bytes
    < HTTP/1.1 500
    < Content-Type: application/json;charset=UTF-8
    < Transfer-Encoding: chunked
    < Date: Sun, 20 Nov 2016 15:36:29 GMT
    < Connection: close
    <
    * Closing connection 0
    {"timestamp":1479656189353,"status":500,"error":"Internal Server Error","exception":"java.lang.IllegalArgumentException","message":"用户已经存在！","path":"/api/accounts/add"}
    ```
    我们发现返回HTTP的状态为500。通常，对于参数错误，我们希望返回状态为400，这需要在服务端做修改：  
    添加Controller通知类
    ```
    @ControllerAdvice
    public class AccountRestControllerAdvice { // 1
        @ResponseBody
        @ExceptionHandler({IllegalArgumentException.class})
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        IllegalArgumentException illegalArgumentExceptionHandler(Exception e) { // 2
            return new IllegalArgumentException(e.getMessage()); // 3
        }
    }
    ```
    1. 使用@ControllerAdvice标注即可，该类会对Controller抛出的异常进行捕获，并返回给前端
    2. 用@ExceptionHandler标注指定需要拦截的异常类型，这里我们拦截IllegalArgumentException异常（可以拦截多种异常类型）；同时我们用@ResponseStatus来指定此异常捕获时对应的HTTP状态码为400
    3. 最终需要返回给前端的异常对象
    重新启动服务，添加已存在用户：
    ```
    curl -v -X POST -H "Content-Type: application/json" http://localhost:8010/api/accounts/add -d '{"username": "zhangsan", "password": "1", "email": "zhangsan@example.com", "firstName": "zhang", "lastName": "san"}'
    
    Note: Unnecessary use of -X or --request, POST is already inferred.
    *   Trying ::1...
    * Connected to localhost (::1) port 8010 (#0)
    > POST /api/accounts/add HTTP/1.1
    > Host: localhost:8010
    > User-Agent: curl/7.49.1
    > Accept: */*
    > Content-Type: application/json
    > Content-Length: 115
    >
    * upload completely sent off: 115 out of 115 bytes
    < HTTP/1.1 400
    < Content-Type: application/json;charset=UTF-8
    < Transfer-Encoding: chunked
    < Date: Sun, 20 Nov 2016 15:53:40 GMT
    < Connection: close
    <
    ...
    ```
    这时，返回HTTP状态码已经变成了400。
    
# 扩展
- 更新Account  

    我们继续修改AccountController，添加Account的更新操作接口  
    
    ```
    ...
    @RequestMapping(method = RequestMethod.PUT, value = "/update")
    public Account update(@RequestBody Account account) { // 9

        Account me = accountRepository.findByUsername(account.getUsername());
        if(me == null){
            throw new IllegalArgumentException("用户不存在！");
        }

        me.setPassword(account.getPassword());
        me.setEmail(account.getEmail());
        me.setFirstName(account.getFirstName());
        me.setLastName(account.getLastName());
        me.setAge(account.getAge());
        me.setGender(account.getGender());

        me = accountRepository.save(me); // 10

        return me;

    }
    ...
    ```
    9. 我们使用PUT请求来进行更新操作
    10. 更新对象并保存
- 删除Account  

    继续修改AccountController，添加Account的删除操作接口
    ```
    ...
    @RequestMapping(method = RequestMethod.DELETE, value = "/{username}")
    public Account delete(@PathVariable String username) { // 11

        Account account = accountRepository.findByUsername(username);
        if(account == null){
            throw new IllegalArgumentException("用户不存在！");
        }

        accountRepository.delete(account); // 12

        return account;
    }
    ...
    ```
    11. 用DELETE请求处理删除操作
    12. 删除对象
- 测试更新和删除
    ```
    curl -v -X PUT -H "Content-Type: application/json" http://localhost:8010/api/accounts/update -d '{"username": "zhangsan", "password": "1", "email": "zhangsan@example.com", "firstName": "zhang", "lastName": "san", "age": 25, "gender": 1}'
    
    Connected to localhost (::1) port 8010 (#0)
    > PUT /api/accounts/update HTTP/1.1
    > Host: localhost:8010
    > User-Agent: curl/7.49.1
    > Accept: */*
    > Content-Type: application/json
    > Content-Length: 139
    >
    } [139 bytes data]
    * upload completely sent off: 139 out of 139 bytes
    < HTTP/1.1 200
    < Content-Type: application/json;charset=UTF-8
    < Transfer-Encoding: chunked
    < Date: Mon, 21 Nov 2016 03:14:21 GMT
    <
    {"id":1,"username":"zhangsan","email":"zhangsan@example.com","firstName":"zhang","lastName":"san","age":25,"gender":1}
    ```
    
    ```
    curl -v -X DELETE http://localhost:8010/api/accounts/zhangsan
    
    Connected to localhost (::1) port 8010 (#0)
    > DELETE /api/accounts/zhangsan HTTP/1.1
    > Host: localhost:8010
    > User-Agent: curl/7.49.1
    > Accept: */*
    >
    < HTTP/1.1 200
    < Content-Type: application/json;charset=UTF-8
    < Transfer-Encoding: chunked
    < Date: Mon, 21 Nov 2016 03:15:20 GMT
    <
    { [123 bytes data]
    {"id":1,"username":"zhangsan","email":"zhangsan@example.com","firstName":"zhang","lastName":"san","age":25,"gender":1}
    ```
    
    