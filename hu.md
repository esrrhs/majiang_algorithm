# 简述
麻将作为国粹，为大众所喜爱，每个地区的玩法都不太一样，但是大部分都会有鬼牌，或者叫癞子，本文主要讲的是带多张鬼牌的胡牌算法。首先，简单说下麻将的基本概念。<br />
[github地址](https://github.com/esrrhs/majiang_algorithm)

# 使用
## maven
``` xml
<dependency>
    <groupId>com.github.esrrhs</groupId>
    <artifactId>majiang_algorithm</artifactId>
    <version>1.0.15</version>
</dependency>
```
``` java
// load
HuTableJian.load(Files.readAllLines(xxx));
HuTableFeng.load(Files.readAllLines(xxx));
HuTable.load(Files.readAllLines(xxx));
// check
boolean isHu = HuUtil.isHu(cards, gui);
List<Integer> tingCards = HuUtil.isTing(cards, gui);
```

# 花色分类
- 普通牌：万筒条，每门有序数从一至九的牌各四张<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/wan.png)
- 风牌：東、南、西、北<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/feng.png)
- 箭牌：中、發、白<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/jian.png)


# 牌型术语
- 连子：一万二万三万<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/lianzi.png)
- 刻子：一筒一筒一筒<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/kezi.png)
- 将：一条一条<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/jiang.png)


# 胡牌公式
- N×连子 + M×刻子 + 1×将
- N>=0, M>=0<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/hu.png)

# 鬼牌
鬼牌的定义就是能够变成任意牌的牌，通常是提前指定或者每次随机决定，比如白板做鬼，如下图：<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/gui.png)<br />
此时白板可以变成**3万**，这手牌已经胡了。

# 遇到的问题
玩过麻将的人会知道，当鬼牌数目比较少时，看胡牌听牌还比较快，如果有多张鬼牌了，别人打一张牌，可能并不会很快的判断是否能胡，比如下图，当别人打了个**6条**，这手牌胡了吗？<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ting.png)<br />实际上能胡的牌有<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/tingde.png)<br />
程序的计算也是同理，所以如果我们只是简单穷举鬼牌的变化可能性，再去计算是否胡牌，那么性能将会很低。本文的目的旨在优化此胡牌算法。

# 解决思路
既然穷举不行，那么我们提前计算好这手牌胡什么，到时候只需要查字典就知道胡的情况了，这个也就是查表法，即空间换时间。<br  />当然，如果把所有牌一起查表，那样数据量又太大了。所以我们根据花色来分别查表，因为万筒条的数据，是一样的。<br />

## 牌型编码
查表的第一步，要对手牌进行编码做key。
- 首先按照花色分成几组，如下图<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianmada.png)<br />分出来万的牌<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianma.png)
- 然后把1万2万5万5万转变成110020000的9位数字，左数第M位是N，说明M万有N张
- 这样万筒条风箭，就有5个数字key。

## 表生成
在生成表的阶段，时间是不值钱的，所以生成方法我们可以任意穷举。
- 首先分为普通、风、箭三张表
- 穷举出所有的key，比如普通表，就是000000000-444420000，因为每一种牌最大4张，且总和不超过14张牌。
- 对于每个key，生成这个key在不同鬼的情况下的胡牌信息列表
- 胡牌信息列表的内容是，在N张鬼的情况下，这个key胡什么牌，并且是否有将
- 例如1万2万5万5万：110020000<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianma.png)<br />
生成的胡牌信息有
<br />1万2万5万5万：鬼0 有将 胡3万（0个鬼的时候，这个牌胡3万，此时有将）
<br />1万2万5万5万：鬼1 无将 胡3万胡5万（1个鬼的时候，这个牌胡3万5万，此时无将）
<br />1万2万5万5万：鬼1 有将 胡了（1个鬼的时候，这个牌已经胡了（鬼变成3万），此时有将）
<br />1万2万5万5万：等等...

## 胡牌算法
有了前面辛苦生成的表格，那么判断胡牌算法就很简单了。
- 对玩家手上的牌进行编码，变成多个key和鬼牌总数N，例如手牌如下<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianmada.png)<br />
得到key：110020000、020000000和鬼牌总数2
- 对每个key查询表，得到对应的胡牌信息列表
- 针对每组列表，过滤掉鬼牌总数>N的项以及没有胡的项，上面的例子就会有<br />
1万2万5万5万：鬼1 有将 胡了<br />
2筒2筒：鬼1 无将 胡了
- 简单递归下，看看几组胡牌信息列表里，是否满足鬼牌总数和只有一个将的约束
- 如果有任意组合满足，则胡了，在上面的例子里，恰好满足条件，于是胡了
- 总耗时：查表耗时*M + 递归M层分配鬼和将的耗时<br />
M是花色数目，M<=5

## 查胡算法
- 与胡牌算法类似，根据key查出胡牌信息列表
- 简单递归下，找出满足鬼的总数和只有一个将的约束时，所有不能胡的胡牌信息里可胡牌的集合，就是这手牌能胡什么牌
- 优化，比如已经知道能胡1万2万3万，那么就不再去递归胡2万（子集）的可能

# 其他
- 目前项目中前后端已经使用此算法，读表前端使用sqlite，后端使用txt加载到内存建立成hashmap
- 可以修改规则重新生成，比如有的麻将东西南也算连子

