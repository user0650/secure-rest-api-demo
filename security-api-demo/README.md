# 概述
本教程在hateoas-api-demo基础上进行改进，为REST服务添加权限管理。  

我们假设有这样的需求：
1. 匿名用户只能调用add接口，对应的是用户注册操作
2. 已经注册的普通用户只能查看、修改、删除自己的信息，对应用户的查看个人信息、修改个人信息、注销账户操作
3. 管理员可以查看所有用户信息

目前没有添加安全控制时，任何人都充当着管理员的角色，即任何人只要知道接口地址、参数，就可以对账户进行操作，这明显是不安全的。  

接下来我们演示如何给REST服务添加上权限控制。
    
# 开始

- 在hateoas-api-demo的基础上，修改pom.xml，添加maven依赖  
```
...
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
...
```

- 添加WebSecurityConfig配置类
```
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter { // 1

    @Override
    protected void configure(HttpSecurity http) throws Exception { // 2
        http
                .httpBasic().and().csrf().disable() // 3
                .authorizeRequests()
                .antMatchers("/api/accounts/add").anonymous() // 4
                .antMatchers("/api/accounts/list").hasAnyRole("ADMIN") // 5
                .antMatchers("/api/accounts/{username}", "/api/accounts/update").hasAnyRole("USER"); // 6
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .inMemoryAuthentication()
                .withUser("user").password("user").roles("USER") // 7
                .and()
                .withUser("admin").password("admin").roles("ADMIN", "USER"); // 8
    }
}
```
1. 用@Configuration、@EnableWebSecurity两个标注指定这是一个配置类，并且开启网络安全模块
2. 重写该方法来配置URL访问权限
3. 为了方便演示，使用基本认证方式，并且关闭csrf保护
4. add接口允许所有人调用，如我们的需求中所说
5. list接口只允许ADMIN角色的用户调用，用于查看所有用户信息
6. 查看、删除、修改用户信息接口只允许USER角色的用户调用
7. 在内存中定义user用户，其角色为USER
8. 在内存中定义admin用户，其角色为ADMIN、USER

# 测试

- 匿名调用add接口  

```
curl -v -X POST -H "Content-Type: application/json" http://localhost:8030/api/accounts/add -d '{"username": "lisi", "password": "1", "email": "lisi@example.com", "firstName": "li", "lastName": "si"}'
```  

可以添加成功

- 匿名调用list接口  

```
curl -v -X GET http://localhost:8030/api/accounts/

{"timestamp":1479719747105,"status":401,"error":"Unauthorized","message":"Full authentication is required to access this resource","path":"/api/accounts/"}
```

可以看到，返回内容提示401错误，即没有权限

- 使用admin来调用list接口  

```
curl -v -X GET http://admin:admin@localhost:8030/api/accounts/
```

此时，能够正常访问此接口

- 使用admin来调用/{username}接口

```
curl -v -X GET http://user:user@localhost:8030/api/accounts/zhangsan
```

能够正常访问

- 使用user来调用/{username}接口  

```
curl -v -X GET http://user:user@localhost:8030/api/accounts/zhangsan
```

能够正常访问

# 完善
到目前为止，我们使用spring-security对接口实现基本的权限控制，但是跟我们的需求还有不符的地方：  

- 当前用户只能访问自己的信息，而目前我们用user用户可以访问任意用户的信息    

接下来，我们继续对这点进行改进

- 用户名与account关联  

我们之前在WebSecurityConfig中定义了两个用户（user和admin）存于内存中，这两个用户与我们的Account没有关系。  

我们希望的是，用于判断权限的用户与我们的Account的username是一个用户，即：  

- http://zhangsan:1@localhost:8030/api/accounts/zhangsan 有权访问
- http://zhangsan:1@localhost:8030/api/accounts/lisi 无权访问

为此，我们需要在内存中定义多个用户，zhangsan、lisi、wangwu...  

当然，这样是可以的，但是我们不会这么做，我们要做的是提供一个Account与User的一个映射关系，让框架自动调用并生成User对象  

修改WebSecurityConfig：
```
...
//    @Autowired
//    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
//        auth
//                .inMemoryAuthentication()
//                .withUser("user").password("user").roles("USER") // 7
//                .and()
//                .withUser("admin").password("admin").roles("ADMIN"); // 8
//    }

    @Autowired
    private AccountUserDetailsService accountUserDetailsService; // 9
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(accountUserDetailsService);
    }
...
```
1. 注释掉之前的configureGlobal，我们不需要在内存中定义User
2. 添加configure方法，注入一个accountUserDetailsService对象

实现AccountUserDetailsService：

```
@Service
public class AccountUserDetailsService implements UserDetailsService { // 1 

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException { // 2
    
        if(username.equals("admin")){
            return new User(username, username, true, true, true, true, AuthorityUtils.createAuthorityList("ADMIN", "USER"));
        }

        Account account = accountRepository.findByUsername(username);
        if(account == null)
            throw new UsernameNotFoundException("用户[" + username + "]不存在！");

        return new User(username, account.getPassword(), true, true, true, true, AuthorityUtils.createAuthorityList("USER")); // 3
    }

}
```
1. 实现UserDetailsService接口
2. 这个接口只有一个方法：loadUserByUsername
3. 根据username查找Account，如果找不到抛出异常，否则将找到的Account转为User对象，我们给所有的User对象制定一个角色名"USER"；如果是admin，我们返回一个固定的admin用户

此时，我们只是解决了多个用户的问题，即我们可以用account对应的用户名、密码来访问/{username}接口了。但是，仍然没有达到只能访问自己的信息效果

为了实现权限控制，我们需要修改AccountController，以account方法为例：  

```
@RequestMapping(method = RequestMethod.GET, value = "/{username}")
public AccountResource account(@PathVariable String username, Principal principal) { // 1
    
    if(!principal.getName().equals(username))
        throw new IllegalArgumentException("无权访问！"); // 2

    Account account = accountRepository.findByUsername(username);
    if(account == null){
        throw new IllegalArgumentException("用户不存在！");
    }

    return new AccountResource(account);
}
```
1. 添加Principal参数，这个参数会自动注入，从Principal中可以得到当前用户的用户名
2. 判断当前用户与传入的username是否相同，若不同，则抛出异常

- 简化接口  

到目前为止，我们对普通用户实现了有效的权限控制，但是我们的接口太过冗余，A用户要访问自己的信息，请求时需要传递两次自己的用户名，我们需要进行简化处理

仍然以AccountController的account为例，既然已经有了principal参数，能够得到当前的用户名，我们就没有必要在URL里再带上/{username}，简化如下：

```
@RequestMapping(method = RequestMethod.GET, value = "/me")
public AccountResource me(Principal principal) { // 1
    String username = principal.getName(); // 2
    Account account = accountRepository.findByUsername(username);
    if(account == null)
        throw new IllegalArgumentException("用户不存在！");
    return new AccountResource(account);
}
```
1. 添加一个接口方法，访问路径为/api/accounts/me，表示访问当前用户自己的信息
2. 直接从Principal中获取用户名，而不用从URL中获取

修改WebSecurityConfig中权限控制：
```
protected void configure(HttpSecurity http) throws Exception {
    http
            .httpBasic().and().csrf().disable()
            .authorizeRequests()
            .antMatchers("/api/accounts/add").anonymous()
            .antMatchers("/api/accounts/me").hasAuthority("USER") // 1
            .antMatchers("/api/accounts/", "/api/accounts/{username}", "/api/accounts/update").hasAuthority("ADMIN") // 2
            ;
}
```
1. USER只能访问新增的个人信息接口
2. ADMIN可以访问原有接口
3. 注意这里hasRole改为了hasAuthority，因为AccountUserDetailsService中产生的User对象定义的是Authority，而非ROLE  

- 测试新接口

```
- curl -v -X GET http://admin:admin@localhost:8030/api/accounts/
- curl -v -X GET http://admin:admin@localhost:8030/api/accounts/zhangsan
- curl -v -X GET http://zhangsan:zhangsan@localhost:8030/api/accounts/me
```

均可正常访问


