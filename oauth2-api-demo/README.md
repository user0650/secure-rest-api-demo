# 概述
本教程在security-api-demo基础上进行改进，为REST服务添加OAuth2权限管理。  

# 什么是OAuth2？

这个我们不打算详细介绍，只说明一下这是一个开放的网络安全协议，提供了4中授权模式：

1. 授权码模式（authorization code）
2. 简化模式（implicit）
3. 密码模式（resource owner password credentials）
4. 客户端模式（client credentials）

具体信息，请网上查阅，有很多介绍说明。

# 为什么要用OAuth2？

最主要的目的就是为了安全。安全问题讲起来很大，我们就以目前的security-api-demo的安全状况来做些说明：  

1. security-api-demo里给REST服务添加了基本授权控制，用户调用接口必须传入用户名、密码，但是明码传递，很容易暴露
2. 如果很不幸，张三的用户名、密码泄露了，任何一个应用都可以拿着这个用户名、密码来获取张三的个人信息

如果使用OAuth2的密码模式给客户端授权，那么授权流程是：

- 客户端界面请求用户输入用户名、密码
- 客户端拿着用户名、用户密码、客户端ID、客户端密码到授权服务器获取token
- 客户端拿着token到REST服务器调用接口
- REST服务器检查token，并返回信息

这样，只有同时知道用户名、用户密码、客户端ID、客户端密码才能获取token

那么，要达到这个效果，似乎我们使用session就可以，为什么我们要使用OAuth2呢？

例如，提供一个/api/accounts/login接口，用户用户登陆，之后返回sessionId，客户端拿着sessionId进行后续操作不是一样的吗？

对于这个问题，有人说负载均衡的话session就不能用了，也有的人说REST面向的是APP客户端，而APP不能使用cookie存储sessionId等等。

其实，这些问题都有技术实现方案，从技术上讲，这些都不是选择OAuth2还是Session的依据。

关键还是要看你的REST服务的使用场景、个人习惯、架构风格，Session与OAuth的设计理念及出发点不同：

如果面向内部系统调用、安全性要求不苛刻、可以记录用户状态，就可以提供个/login接口进行登陆操作，服务器通过Session判断权限，只不过这样你的REST服务会被指责为unREST架构；
如果REST服务作为云上的公共资源，用户无状态（没必要记录状态或没条件记录状态），推荐使用OAuth2授权，其4种授权模式可根据实际需求来选择。

当然，本示例中我们还是用要OAuth2的。
    
# 开始

- 在security-api-demo的基础上，修改pom.xml，添加maven依赖  
```
...
<dependency>
    <groupId>org.springframework.security.oauth</groupId>
    <artifactId>spring-security-oauth2</artifactId>
</dependency>
...
```

- 添加OAuth2Config
```
@Configuration
@EnableAuthorizationServer
public class OAuth2Config extends AuthorizationServerConfigurerAdapter { // 1

    @Autowired
    private AuthenticationManager authenticationManager; // 2

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .authenticationManager(this.authenticationManager); // 3
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients
                .inMemory()

                .withClient("ios-client")
                .authorizedGrantTypes("password", "refresh_token")
                .authorities("IOS_USER")
                .scopes("read", "write")
                .resourceIds("api-accounts")
                .accessTokenValiditySeconds(60 * 60 * 24)
                .secret("ios"); // 4
    }

}
```
1. 配置OAuth2服务器，通过@EnableAuthorizationServer注解开启授权服务器功能
2. 因为我们使用的是password授权类型，所以需要注入此对象，这个对象是由AuthenticationManagerBuilder创建，而AuthenticationManagerBuilder是我们在WebSecurityConfig中配置的，我们之前为其设置了AccountUserDetailsService，用于做用户映射
3. 设置authenticationManager
4. 基于内存的客户端对象定义，客户端ID为ios-client，客户端密码为ios，授权方式为password和refresh_token（可取的值为"password"、"authorization_code"、"refresh_token"、"implicit"，对应前边所说的四种授权类型），权限类型为IOS_USER，权限范围是read、write，有权的资源ID为api-accounts，token有效时间为1天

- 添加ResourceServerConfig
```
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter { // 1

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/api/accounts/me").access("#oauth2.clientHasRole('IOS_USER')"); // 2
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources.resourceId("api-accounts"); // 3
    }
}
```
1. 配置资源服务器，通过@EnableResourceServer注解开启资源服务器。通常，资源服务器与授权服务器是不同的应用，这里我们为了方便演示，都放到一个应用中
2. 配置资源服务器权限控制，这里的设置表示允许oauth2中定义的有IOS_USER角色的client访问该地址。这里我们使用了Spring EL表达式，表达式中oauth2都有哪些函数可调用，可参考OAuth2SecurityExpressionMethods类，其中定义的方法就是表达式能够支持的函数
3. 给资源服务器指定一个ID，这个ID就是前边OAuth2Config中定义client时为其设置的ID，在多个资源服务器共用一个授权服务器时就需要用这个标识来指定客户端能够访问哪些的资源服务器

# 测试

- 调用原有接口

尝试调用原来的接口，即在WebSecurityConfig中已经授权过的/api/accounts/me接口，会发现不能访问了，提示无权

这是因为加入OAuth2依赖后，OAuth2权限控制机制的优先级高于我们之前设置的Basic授权机制，因此想要能够访问资源，必须先获取token，再拿着token去请求资源

- 获取token  

```
curl -v -X POST 'http://ios-client:ios@localhost:8040/oauth/token?grant_type=password&username=zhangsan&password=1'
```

返回

```
{"access_token":"03ccf129-9528-418b-84e2-c51a745926cd","token_type":"bearer","expires_in":86399,"scope":"read write"}
```

1. access_token - 后续请求需要传递的token值
2. token_type - 表示此token的类型
3. expires_in - 表示此token的剩余有效时间，单位为秒
4. scope - 表示此token的访问范围，资源服务器可根据次scope做不同控制

- 使用token访问资源

```
curl -v -X GET http://localhost:8040/api/accounts/me -H "Authorization: bearer <access_token>"
```

将access_token替换，可成功调用此接口

# 完善

- 去掉WebSecurityConfig

我们发现，WebSecurityConfig中配置的权限控制已经被ResourceServerConfig配置的权限控制取代了；同时，WebSecurityConfig配置的AccountUserDetailsService我们也可以将其放到OAuth2Config中来配置：

```
...
@Autowired
private AccountUserDetailsService accountUserDetailsService; // 1

@Autowired
private AuthenticationManager authenticationManager;

@Override
public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
    endpoints
            .authenticationManager(this.authenticationManager)
            .userDetailsService(accountUserDetailsService); // 2
}
...
```
1. 注入AccountUserDetailsService
2. 设置AccountUserDetailsService到endpoints中

此时，WebSecurityConfig已经完全无用了，可以将其移除

- token存储问题

我们知道，授权服务器负责发放token，资源服务器负责校验token，因此这两个服务器需要共享token数据。

我们现在的token是存储在内存中，之所以授权服务器、资源服务器都能正常访问，是因为这两个服务器是同一个应用，能够共享内存资源（token）。

而实际中我们的授权服务器、资源服务器经常是不同的应用，那么如何能够保证授权服务器与资源服务器都能访问到token呢？

最简单的办法是将token存于数据中，让授权服务器、资源服务器使用同一个数据库。

为了实现此目的，可以使用spring-oauth2已经封装好的JdbcTokenStore，构造JdbcTokenStore只需要为其指定数据源即可。

定义AccountTokenStore：

```
@Component
public class AccountTokenStore extends JdbcTokenStore { // 1

    @Autowired
    private DataSource dataSource; // 2

    public AccountTokenStore(DataSource dataSource) {
        super(dataSource);
    }
}
```
1. 继承JdbcTokenStore，并使用@Component注解，使其被spring容器管理
2. 为其注入dataSource，这里使用的是默认的tomcat数据源，数据源配置信息见application.properties中spring.datasource.*

修改OAuth2Config：

```
@Autowired
private AccountTokenStore accountTokenStore; // 1

@Override
public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
    endpoints
            .authenticationManager(this.authenticationManager)
            .userDetailsService(accountUserDetailsService)
            .tokenStore(accountTokenStore) // 2
    ;
}
```
1. 注入刚才定义的AccountTokenStore
2. 设置tokenStore为AccountTokenStore

对应地，修改ResourceServerConfig：

```
@Override
public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
    resources
            .resourceId("api-accounts")
            .tokenStore(accountTokenStore)
    ;
}
```

与OAuth2Config类似，注入并设置tokenStoren为AccountTokenStore

除此，我们需要在数据库中建立对应的数据表：oauth_access_token、oauth_refresh_token （查看JdbcTokenStore源码，可以看出用到了这两张表，同时可以看出表的结构）

```
drop table if exists oauth_access_token;
create table oauth_access_token (
  token_id VARCHAR(255),
  token LONG VARBINARY,
  authentication_id VARCHAR(255) PRIMARY KEY,
  user_name VARCHAR(255),
  client_id VARCHAR(255),
  authentication LONG VARBINARY,
  refresh_token VARCHAR(255)
);

drop table if exists oauth_refresh_token;
create table oauth_refresh_token (
  token_id VARCHAR(255),
  token LONG VARBINARY,
  authentication LONG VARBINARY
);
```

重启应用，请求/oauth2/token获取token，然后查看数据库数据，发现oauth_access_token有了token记录。这时使用token访问/api/accounts/me也能正常访问

另外一张表oauth_refresh_token是token更新操作时用的，我们这里不再演示

- client存储问题

我们观察OAuth2Config中对Client对象的定义，发现与之前我们在WebSecurityConfig的configureGlobal方法中定义内存User对象时很相似

你可能已经猜到了，类似的我们可以通过一个ClientDetailsService，来实现从其他途径（例如数据库中）获取client信息，然后将其映射为ClientDetails对象

示例：

```
public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
    clients.withClientDetails(new ClientDetailsService() {
        @Override
        public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
            BaseClientDetails client = new BaseClientDetails();
    
            client.setClientId("ios-client");
            client.setClientSecret("ios");
    
            List<String> resourceIds = new ArrayList<>();
            resourceIds.add("api-accounts");
            client.setResourceIds(resourceIds);
    
            List<String> scopes = new ArrayList<>();
            scopes.add("read");
            scopes.add("write");
            client.setScope(scopes);
    
            List<String> authorizedGrantTypes = new ArrayList<>();
            authorizedGrantTypes.add("password");
            authorizedGrantTypes.add("refresh_token");
            client.setAuthorizedGrantTypes(authorizedGrantTypes);
    
            client.setAuthorities(AuthorityUtils.createAuthorityList("IOS_USER"));
            client.setAccessTokenValiditySeconds(60 * 60 * 24);
    
            return client;
        }
    });
}
```

这里我们直接在ClientDetailsService中创建一个BaseClientDetails，并设置各属性，效果与之前的内存中创建Client一样

实际使用时，可根据情况，从数据库或者其他地方获取Client信息，并转换成BaseClientDetails


# 关于Authority和Role

在security-api-demo示例中，我们定义内存User时，使用下面的语句：

```
auth.inMemoryAuthentication().withUser("user").password("user").roles("USER")
```

其中，为user用户设置的角色为"USER"

对应的，我们设置http权限验证时用下面语句：

```
http.antMatchers("/api/accounts/me").hasRole("USER")
```

指定角色为"USER"的用户可以访问/api/accounts/me接口

当我们使用UserDetailsService生成User对象时，我们有如下代码：

```
return new User(username, account.getPassword() != null ? account.getPassword() : username, AuthorityUtils.createAuthorityList("USER"))
```

我们在构造方法中给user设置了Authority为"USER"

这时，我们需要修改http的权限验证代码为：

```
http.antMatchers("/api/accounts/me").hasAuthority("USER")
```

那么，Authority和Role到底有什么区别和联系呢？

可以这么理解：Role是Authority的载体，Authority是Role的属性，最简情况下，它们之间存在一一对应的关系

例如，我们可以说：图书管理员用户拥有管理图书的权限，也可以说：拥有管理图书的权限的用户我们叫他（她）图书管理员

在spring-security中，它们之间有如下关系：一个用户有ROLE_X权限，那么他（她）的角色就是X（注意，默认这个对应关联关系是以“ROLE_”作为前缀）

可以做如下试验：

1. 修改UserDetailsService，生成User时设置其权限为ROLE_USER  

```
return new User(username, account.getPassword() != null ? account.getPassword() : username, AuthorityUtils.createAuthorityList("ROLE_USER"))
```

2. 修改http的权限验证代码为：
   
```
http.antMatchers("/api/accounts/me").hasRole("USER")
```

3. 测试/api/accounts/me接口调用，发现也能够正常调用，这就是spring-security权限校验时为我们做的自动转化

而在spring-oauth2中就更简单了，Authority和Role完全相同，可以查看我们的OAuth2Config和ResourceServerConfig的代码

OAuth2Config中我们设置client的权限为IOS_USER，而ResourceServerConfig中我们使用的表达式为#oauth2.clientHasRole('IOS_USER')


