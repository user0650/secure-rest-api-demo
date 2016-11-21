# 概述
本教程基于rest-api-demo进行改进，在基础的REST服务基础上添加HATEOAS约束。

HATEOAS（Hypermedia as the engine of application state）是 REST 架构风格中最复杂的约束，也是构建成熟 REST 服务的核心。它的重要性在于打破了客户端和服务器之间严格的契约，使得客户端可以更加智能和自适应，而 REST 服务本身的演化和更新也变得更加容易。

# 为什么要HATEOAS？
想象一下这样的场景：
    后台公布了一组接口
        - [http://localhost:8020/api/accounts/add]() (添加账户)
        - [http://localhost:8020/api/accounts/{username}]() (查看或删除账户)
        - [http://localhost:8020/api/accounts/update]() (更新账户)
    客户端根据这一组接口进行开发，就很依赖这组接口的地址
    如果有一天后台进行了重构，把地址改为
        - [http://localhost:8020/api/accounts/post]() (添加账户)
        - [http://localhost:8020/api/accounts/get/{username}]() (查看账户)
        - [http://localhost:8020/api/accounts/delete/{username}]() (删除账户)
        - [http://localhost:8020/api/accounts/put]() (更新账户)
    那么所有调用这组接口的客户端都需要修改代码
而HATEOAS的目的就是为了让客户端与服务端最大程度解耦，假设服务端只公开一个接口
    - [http://localhost:8020/api/accounts/list] (查看账户列表)
返回的内容中包含每一个账户的添加、删除、修改、查看等操作的链接，那么客户端就可以只依赖这一个接口，服务端只要保证这个接口不变即可
    
# 开始

- 在rest-api-demo的基础上，修改pom.xml，添加maven依赖
    ```
    ...
    <dependency>
        <groupId>org.springframework.hateoas</groupId>
        <artifactId>spring-hateoas</artifactId>
    </dependency>
    ...
    ```
- 添加AccountResource类，封装Account
    ```
    public class AccountResource extends ResourceSupport { // 1
    
        private final Account account;
    
        public AccountResource(Account account) {
            this.account = account;
            this.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(AccountController.class).add(null)).withRel("add"));
            this.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(AccountController.class).account(account.getUsername())).withSelfRel());
            this.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(AccountController.class).update(null)).withRel("update"));
            this.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(AccountController.class).delete(account.getUsername())).withRel("delete"));
            // 2
        }
    
        public Account getAccount() {
            return account;
        }
    
    }
    ```
    1. 封装类继承ResourceSupport
    2. 添加4个链接，分别是Account的增加、查看、修改、删除操作的链接
- 修改AccountController，添加list接口
    ```
    @RequestMapping(method = RequestMethod.GET, value = "/")
    public Resources<AccountResource> list() { // 1
        Iterable<Account> accounts = accountRepository.findAll();
        List<AccountResource> accountList = new ArrayList<>();
        accounts.forEach(account -> {
            accountList.add(new AccountResource(account));
        });
        // 2
        return new Resources<>(accountList);
    }
    ```
    1. 定义list接口，用于获取Account列表
    2. 查询列表，并封装成AccountResource返回
    
# 测试
- 调用list接口
    ```
    curl -v -X GET http://localhost:8020/api/accounts/
    
    {
        "links":[],
        "content":
        [
            {
                "account":{"id":3,"username":"zhangsan","email":"zhangsan@example.com","firstName":"zhang","lastName":"san","age":null,"gender":null},
                "links":
                [
                    {"rel":"add","href":"http://localhost:8020/api/accounts/add"},
                    {"rel":"self","href":"http://localhost:8020/api/accounts/zhangsan"},
                    {"rel":"update","href":"http://localhost:8020/api/accounts/update"},
                    {"rel":"delete","href":"http://localhost:8020/api/accounts/zhangsan"}
                ]
            }
        ]
    }
    ```
    可以看到返回的结果中除了Account信息外，多了links内容，这些link分别为account的增加、查看、修改、删除操作的链接
    这样，客户端编程时就可以根据links来对Account资源进行操作

# 进阶
- 错误处理
    之前，我们在服务器端将错误信息进行拦截，并抛给了客户端，这样存在的问题是不同的异常内容格式可能会存在一些差异，而且有些异常信息不那么友好
    我们可以使用hateoas提供的VndErrors来封装各类异常信息
    ```
    @ControllerAdvice
    public class AccountRestControllerAdvice {
    
        @ResponseBody
        @ExceptionHandler({IllegalArgumentException.class})
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        VndErrors illegalArgumentExceptionHandler(Exception e) { // 1
            return new VndErrors("error", e.getMessage()); // 2
        }
    
    }
    ```
    1. 修改返回异常的类型为VndErrors
    2. 返回VndErrors对象,设置级别为"error"，内容为异常信息
    
    调用删除用户接口，传入一个不存在的用户名，使其抛出异常
    ```
    curl -v -X DELETE http://localhost:8020/api/accounts/lisi
    
    [
        {"logref":"error","message":"用户不存在！","links":[]}
    ]
    ```
    可以看到，返回的错误信息变成我们在服务器端设置的错误信息
    
# 扩展
- 修改add、update、delete、account接口，返回结果用AccountResource封装
    ```
    略。参考hateoas-api-demo源码。
    ```
