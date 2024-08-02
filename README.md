<p align="center"> 
  通过JSON生成SQL查询语句
</p>
<p align="center">
  <a href="https://central.sonatype.com/artifact/tech.kuaida/JSQL">
    <img alt="maven" src="https://img.shields.io/maven-central/v/tech.kuaida/JSQL">
  </a>

  <a href="https://opensource.org/licenses/MIT">
    <img alt="code style" src="https://img.shields.io/badge/license-MIT-4EB1BA.svg">
  </a>
</p>

## 简介 | Intro
JSQL是快搭平台为了能方便AI自动生成系统，借助JSON的编写规则所设计的一种查询语句。通过简单的编写可以快速的将后台数据按照编写的规则调取出来。不过在使用过程中，用户需要注意数据的查询安全性，对于一些敏感字段JSQL还是会自动给查询出来，这需要开发人员在后端代码中主动的去过滤掉查询结果中的敏感字段，从而防止关键数据外泄。

## 主要特点 | Advantages
### 配置简单
在pom.xml中加入以下依赖即可使用。 如果需要查看或修改源码，甚至可以直接将JSQL的源码拷到项目目录里就可以直接使用了。
```xml
<dependency>
    <groupId>tech.kuaida</groupId>
    <artifactId>JSQL</artifactId>
    <version>1.0.1</version>
</dependency>
```
### 操作便捷
JSQL的最大特点是直接将JSON对象进行解析，然后按照一定的格式规则，快速的将JSON对象转换成SQL查询语句。对于对象之间的关联关系，它会自动查询相关的JPA实体类来进行解析，从而建立关联查询。它的整个架构都是建立在这个基础上，这是它与其它SQL生成工具最大的区别。通过JSON来转化成SQL并进行数据库查询，大大降低了前后端的开发工作，使前端在只要了解对象模型的情况下就可以快速进行数据查询，而后端开发工程师只需要对敏感数据进行限制，即可快速完成系统的开发。

现在我们有三个业务模型实体类：<a href="https://github.com/kuaida-tech/JSQL/blob/main/example/src/modules/customer/Customer.java">customer(客户)</a>、<a href="https://github.com/kuaida-tech/JSQL/blob/main/example/src/modules/salesperson/Salesperson.java">salesperson(销售员)</a>和<a href="https://github.com/kuaida-tech/JSQL/blob/main/example/src/modules/transaction/Transaction.java">transaction(交易数据)</a>，我们想获取系统类所有销售员，以及销售员所关联的客户信息。我们将采用下面的JSON对象来生成查询SQL

#### JSON代码
```
{
    salesperson: {
        name: null,
        customer: {
            name: null,
            phone: null
        }
    }
}
```
#### 后端代码
```java
JSONObject jsonObject = JSONObject.fromObject(json);
//创建实例时传入JPA类所在包的完整路径和JSON对象
//示例文件夹(example)中，JPA类直接放在modules包下，所以传入参数"modules",如果还有上级包则传入包名写为"xxxx.xxxx.modules"
SelectBuilder selectBuilder = new SelectBuilder("modules", jsonObject);
System.out.println(selectBuilder.toString());
```
#### 输出结果：
```sql
SELECT
salesperson.NAME AS "salesperson.name",
customer.NAME AS "salesperson.customer.name",
customer.phone AS "salesperson.customer.phone",
customer.id AS "salesperson.customer.id",
salesperson.id AS "salesperson.id"
FROM
salesperson AS salesperson
LEFT JOIN customer AS customer ON salesperson.id = customer.salesperson_id
```

### 兼容主要JPA注解
通过上面的例子可以看出，快搭JSQL建立对象之间的关联关系，主要是通过对JPA实体类进行解析。目前快搭JSQL支持JPA实体类以下注解标签
```java
@Entity, @Transient, @UniqueConstraint, @GenerationType, @Id, @Index, @SequenceGenerator, 
@GeneratedValue, @Table, @Column, @TableGenerator, @Version, @Enumerated, @Convert, @Temporal
```
## 操作手册 | Manual
### 字段获取
上面简单的示例中已经描述过了关于对象字段是怎么获取的，这里我们再进行一次详细的介绍:
1、JSON对象名与JPA实体类同名，首字母小写。在JSON对象中带上想要获取的字段名称，例如销售员有name、email、phone三个字段，以及一个salespersonIdCustomer关联关系
```
//获取所有销售员姓名
{
    salesperson: {
        name: null
    }
}
```
```
//获取所有销售员姓名以及与其关联的客户姓名和客户联系电话
{
    salesperson: {
        name: null,
        customer: {
            name: null,
            phone: null
        }
    }
}
```

### 字段指令
快搭JSQL中JSQL所谓的指令是指在字段获取的同时，针对当前字段做出特殊处理。指令是由字段名称 + $ + 指令构成，以对字段别名(重命名)为例:
JSON数据:
```
{
    salesperson: {
        name$AsalespersonName: null,
    }
}
```

生成的查询SQL:
```sql
SELECT
	salesperson.NAME AS "salesperson.salespersonName",
	salesperson.id AS "salesperson.id" 
FROM
	salesperson AS salesperson
```
可根据生产的SQL看出，salesperson的name字段用上了SQL的alias语法，将结果字段名指定成了salesperson.salespersonName。快搭JSQL现已支持的指令如下：

「$A」对应SQL中的Alias(别名):
```
{
    salesperson: {
        name$AsalespersonName: null,
    }
}
```

生成的SQL:
```sql
SELECT
	salesperson.NAME AS "salesperson.salespersonName",
	salesperson.id AS "salesperson.id" 
FROM
	salesperson AS salesperson
```
「$F」调用MYSQL部分内置函数，针对有参数的函数可用「__」两根下划线来进行分隔:
1、带参(数字)函数调用
```
{
    salesperson: {
        name$FSUBSTRING__1__5: null,
    }
}
```
生成的SQL:
```sql
SELECT
	SUBSTRING( salesperson.NAME, 1, 5 ) AS "salesperson.name",
	salesperson.id AS "salesperson.id" 
FROM
	salesperson AS salesperson
```

2、带参(字符串)函数调用
```
{
    salesperson: {
        name$FREPLACE__'123'__'hello': null,
    }
}
```
生成的SQL:
```sql
SELECT REPLACE
	( salesperson.NAME, '123', 'hello' ) AS "salesperson.name",
	salesperson.id AS "salesperson.id" 
FROM
	salesperson AS salesperson
```

3、字段参与函数调用
```
{
    salesperson: {
        name$FCONCAT__email: null,
        email: null
    }
}
```

生成的SQL:
```sql
SELECT
	CONCAT( salesperson.NAME, email ) AS "salesperson.name",
	salesperson.email AS "salesperson.email",
	salesperson.id AS "salesperson.id" 
FROM
	salesperson AS salesperson
```

「$G」等同于Sql的GROUP BY:
```
{
    salesperson: {
        name$G: null,
        transaction: {
            amount$FSUM: null
        }
    }
}
```
生成的SQL:
```sql
SELECT
	salesperson.NAME AS "salesperson.name",
	SUM( TRANSACTION.amount ) AS "salesperson.transaction.amount",
	TRANSACTION.id AS "salesperson.transaction.id",
	salesperson.id AS "salesperson.id" 
FROM
	salesperson AS salesperson 
GROUP BY
	salesperson.NAME
```

「$S」指定字段排序，其后跟ASC或DESC来决定是升序还是降序:
```
{
    salesperson: {
        name$SASC: null,
    }
}
```

生成的SQL:
```sql
SELECT
	salesperson.NAME AS "salesperson.name",
	salesperson.id AS "salesperson.id" 
FROM
	salesperson AS salesperson 
ORDER BY
	salesperson.NAME ASC
```

### 条件指令
JSQL所谓的查询指令就是指在获取目标数据时带上额外的查询条件，从而过滤掉多余的信息，只获取符合条件的相关数据

「$HAVING」为聚合数据进行过滤:
```
{
    customer: {
        name: null,
        transaction: {
            amount$FSUM$Aamount:null
        },
        $HAVING: {
            amount$GT: 1000
        }
    }
}
```

生成的SQL:
```sql
SELECT
	customer.NAME AS "customer.name",
	SUM( TRANSACTION.amount ) AS "amount",
	TRANSACTION.id AS "customer.transaction.id",
	customer.id AS "customer.id" 
FROM
	customer AS customer
	LEFT JOIN TRANSACTION AS TRANSACTION ON customer.id = TRANSACTION.customer_id 
HAVING
	( amount > '1000' )
```

「$AND」和运算、「$OR」或运算:
```
{
    customer: {
        name: null,
        transaction: {
            amount:null,
            $AND: {
                amount$GT: 800
            }
        },
        $AND: {
            name$EQ: "张三"
        }
    }
}
```

在上述示例中出现了$EQ、$GT这些指令，这些指令代表的具体含义如下:
- $EQ: 等于
- $NE: 不等于
- $GT: 大于
- $GE: 大于等于
- $LT: 小于
- $LE: 小于等于
- $ISNULL: 是否为空
- $NOTNULL: 不为空
- $LIKE: 模糊匹配
- $NOTLIKE: 模糊匹配

### 关联查询
在日常操作中，我们还经常遇到需要对来自两个或多个表的数据，通过基于一个或多个相关列之间的关系将它们连接在一起进行查询
「$JI」等同于Sql的INNER JOIN内连接:
```
{
    salesperson: {
        name: null,
        phone: null,
        customer: {
            salespersonId$JIsalesperson__id: null,
            name: null,
            phone: null,
            totalSpent: null
        }
    }
}
```
生成的SQL:
```sql
SELECT
	salesperson.NAME AS "salesperson.name",
	salesperson.phone AS "salesperson.phone",
	customer.salesperson_id AS "customer.salespersonId",
	customer.NAME AS "customer.name",
	customer.phone AS "customer.phone",
	customer.total_spent AS "customer.totalSpent",
	customer.id AS "customer.id",
	salesperson.id AS "salesperson.id" 
FROM
	salesperson AS salesperson
	INNER JOIN customer AS customer ON customer.salesperson_id = salesperson.id
```
同样JSQL也支持其他的关联查询指令:
- $JL: LEFT JOIN 左连接
- $JR: RIGHT JOIN 右连接
- $JF: FULL OUTER JOIN 全连接

## 期望 | Futures

欢迎发issue提出更好的意见或提交PR，帮助完善项目
