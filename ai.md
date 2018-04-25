# 简述
打麻将，需要运气，也需要脑力。作为玩家，需要搭好牌架子，然后一张一张的摸牌，最后达到听牌，最终胡牌。<br />
本文讲述的即是AI如何尽量做到高智商的打麻将。其中摸牌我们是控制不了的，所以就在打牌上下手。<br />
首先还是先复习下麻将玩法。<br />
[github地址](https://github.com/esrrhs/majiang_algorithm)

# 使用
## maven
``` xml
<dependency>
    <groupId>com.github.esrrhs</groupId>
    <artifactId>majiang_algorithm</artifactId>
    <version>1.0.10</version>
</dependency>
```
``` java
// load
AITableJian.load(Files.readAllLines(xxx));
AITableFeng.load(Files.readAllLines(xxx));
AITable.load(Files.readAllLines(xxx));
// 出牌
int card = AIUtil.outAI(cards, gui);
// 碰
boolean isPeng = AIUtil.pengAI(cards, gui, pengCard, 0.d);
// 杠
boolean isGang = AIUtil.gangAI(cards, gui, gangCard, 0.d);
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
在本文中，不需要考虑鬼牌，因为不会打鬼牌，所以我们只需要把其他牌做的完美，就可以随便和鬼牌达到听牌胡牌。

# 案例分析
我们先举几个直观的例子，看看人是怎么思考出牌的
- 1万2万3万2条，很简单打这个单的2条，剩下的就是连子<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai1.png)
- 1万2万3万1条1条7条，1条做将，打这个单的7条<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai2.png)
- 1万2万3万2筒3筒1条2条，要想组连子，需要3条、1筒4筒，拆这个1条2条，打1条好，2条万一摸到3条4条还有机会<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai3.png)


# 解决思路
从上面的例子可以看出来，打牌的过程，其实就是评估打完之后的牌面，取一个最佳牌面。<br  />
也就是说，算法变成了评估牌面积分的算法，越高说明牌越好，也说明这副牌可以胡的概率更高。<br />

## 评估方法
为了评价这副牌的积分，也就是胡牌的概率，我们可以给他再摸N张牌，看看胡牌情况。参考如下示例，可以很直观得出牌面积分：1万2万3万 > 1万2万3万2条3条 > 1万2万3万2条。
- 1万2万3万<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai4.png)<br />
已经胡了，胜率为1
- 1万2万3万2条<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai1.png)<br />
只摸1张牌，那么只有当摸2条的时候，才会赢，胜率为1/9*摸条的概率，此时有将。
- 1万2万3万2条3条<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai5.png)<br />
只摸1张牌，那么只有当摸1条4条的时候，才会赢，胜率为2/9*摸条的概率，此时无将。


## 表格生成
有了评估方法后，我们只需要对每个花色的手牌，分配N张牌给他，然后计算胜率，就可以知道牌面积分。<br />
不过考虑到计算量太大，所以我们可依然使用查表法，提前计算好，方便快速查找。<br />
当然，这里的问题就是不会去参考当前桌子剩余的牌，不过相比计算效率，这一点牺牲是可以接受的。

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
- 穷举出所有的key，比如普通表，就是000000000-444200000，因为每一种牌最大4张，且总和不超过14张牌。
- 对于每个key，给定输入N张牌，生成这个key在有将无将下的胜率。
- 例如1万2万5万5万：110020000<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianma.png)<br />
生成的胜率信息有
<br />1万2万5万5万：无将 0.006099681251811069（这手牌如果不做将，能胡的概率是0.006）
<br />1万2万5万5万：有将 0.03433398152649489（这手牌如果做将，能胡的概率是0.03，因为有现成5万的将，只需要3万就能胡）


## 评估算法
有了前面辛苦生成的表格，那么评估积分算法就很简单了。
- 对玩家手上的牌进行编码，变成多个key和鬼牌总数N，例如手牌如下<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/bianmada.png)<br />
得到key：110020000、020000000和鬼牌总数2，同时对于没有的花色，也补上key。
- 对每个key查询表，得到对应的胜率信息列表
- 上面的例子就会有<br />
1万2万5万5万：无将 0.006<br />
1万2万5万5万：有将 0.03<br />
2筒2筒：无将 0.02<br />
2筒2筒：有将 1.0<br />
条子（无）：无将 1.0<br />
条子（无）：有将 0.05<br />
风牌（无）：无将 1.0<br />
风牌（无）：有将 0.05<br />
箭牌（无）：无将 1.0<br />
箭牌（无）：有将 0.05<br />
- 简单递归下，计算胜率总和的最大值，并且满足有且只有1个将，本例中，将取筒子，max=4.006<br /> 递归M层分配鬼和将的耗时<br />
M是花色数目，M<=5

## 出牌算法
- 遍历手上的非鬼牌，计算排除掉这张牌后的牌面积分最大值，这张牌就是要打的牌。
- 如果打出能听牌了，就取一个听牌最多的牌打出去。
- 考虑如下的牌<br />
![image](https://github.com/esrrhs/majiang_algorithm/raw/master/img/ai6.png)<br />
打出2万3万，积分为3.02<br />
打出1条，积分为2.07<br />
打出东，积分为4.02<br />
于是这手牌打出了东。
