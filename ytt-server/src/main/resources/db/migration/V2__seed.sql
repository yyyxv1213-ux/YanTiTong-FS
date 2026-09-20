-- ============================================================
-- V2 初始化种子数据
-- 1) 内置角色 ADMIN/STUDENT 与权限点
-- 2) 题目分类（由内置题库章节归集生成）
-- 3) 内置题库 41 道 408 原创题目（options/answer 以 JSON 存储）
-- ============================================================

-- 内置角色
INSERT INTO `sys_role` (`id`, `code`, `name`) VALUES
(1, 'ADMIN', '系统管理员'),
(2, 'STUDENT', '考研学员');

-- 后台权限点
INSERT INTO `sys_permission` (`id`, `code`, `name`, `type`) VALUES
(1, 'admin:user:list', '用户查询', 'api'),
(2, 'admin:user:create', '新增用户', 'api'),
(3, 'admin:user:update', '编辑用户', 'api'),
(4, 'admin:user:delete', '删除用户', 'api'),
(5, 'admin:user:status', '启用/禁用用户', 'api'),
(6, 'admin:user:reset-password', '重置密码', 'api'),
(7, 'admin:user:assign-role', '分配角色', 'api'),
(8, 'admin:question:list', '题目查询', 'api'),
(9, 'admin:question:create', '新增题目', 'api'),
(10, 'admin:question:update', '编辑题目', 'api'),
(11, 'admin:question:delete', '删除题目', 'api'),
(12, 'admin:category:manage', '分类管理', 'api');

-- ADMIN 角色拥有全部权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 1),
(1, 2),
(1, 3),
(1, 4),
(1, 5),
(1, 6),
(1, 7),
(1, 8),
(1, 9),
(1, 10),
(1, 11),
(1, 12);

-- 题目分类（按科目归集章节）
INSERT INTO `question_category` (`id`, `subject`, `name`, `sort`) VALUES
(1, 'ds', '线性表', 1),
(2, 'ds', '栈和队列', 2),
(3, 'ds', '树与二叉树', 3),
(4, 'ds', '图', 4),
(5, 'ds', '排序', 5),
(6, 'ds', '查找', 6),
(7, 'co', '系统概述', 1),
(8, 'co', '数据的表示', 2),
(9, 'co', '存储器层次结构', 3),
(10, 'co', '中央处理器', 4),
(11, 'co', '输入输出系统', 5),
(12, 'os', '进程管理', 1),
(13, 'os', '进程同步', 2),
(14, 'os', '死锁', 3),
(15, 'os', '内存管理', 4),
(16, 'os', '设备管理', 5),
(17, 'os', '文件管理', 6),
(18, 'cn', '体系结构', 1),
(19, 'cn', '物理层', 2),
(20, 'cn', '数据链路层', 3),
(21, 'cn', '网络层', 4),
(22, 'cn', '传输层', 5),
(23, 'cn', '应用层', 6);

-- 内置题库（41 题）
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (1, 'single', 'ds', 1, '线性表', 1, '在带头结点的单链表中，已知指针 p 指向表中某结点，指针 s 指向待插入的新结点。若要将 s 插入到 p 所指结点之后，正确的语句序列是（ ）。', '["p.next = s；s.next = p.next","s.next = p.next；p.next = s","p.next = s.next；s.next = p","s.next = p；p.next = s"]', '[1]', '后插操作必须先将新结点的指针指向 p 的原后继，再令 p 指向新结点。若先执行 p.next=s，则原后继地址丢失，链表将断裂。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (2, 'single', 'ds', 2, '栈和队列', 1, '栈作为一种操作受限的线性表，其插入（入栈）和删除（出栈）操作的位置分别是（ ）。', '["都在栈顶进行","都在栈底进行","入栈在栈顶，出栈在栈底","可在任意位置进行"]', '[0]', '栈遵循"后进先出（LIFO）"原则，所有插入和删除都只能在栈顶一端进行。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (3, 'single', 'ds', 2, '栈和队列', 2, '采用"牺牲一个存储单元"方法的循环队列容量为 M，front 指向队头元素，rear 指向队尾元素的下一位置，则队满的判断条件是（ ）。', '["rear == front","(rear + 1) % M == front","(front + 1) % M == rear","rear - front == M"]', '[1]', '循环队列牺牲一个单元以区分队空（rear==front）与队满，队满条件为 (rear+1)%M==front，队列中最多存放 M-1 个元素。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (4, 'single', 'ds', 3, '树与二叉树', 1, '深度为 k（k≥1）的二叉树，最多拥有的结点数为（ ）。', '["2^k","2^k - 1","2^(k-1)","2^k + 1"]', '[1]', '满二叉树每层结点数为 1,2,4,…,2^(k-1)，等比数列求和得到总数 2^k-1。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (5, 'single', 'ds', 3, '树与二叉树', 2, '下列关于哈夫曼（Huffman）树的叙述，正确的是（ ）。', '["哈夫曼树是严格二叉树，树中不存在度为 1 的结点","哈夫曼树一定是完全二叉树","给定相同权值的叶子集合，构造出的哈夫曼树唯一","哈夫曼树中叶子结点数等于内部结点数"]', '[0]', '哈夫曼树每次合并两棵子树，故没有度为1的结点；叶子数 n0 与内部结点数 n2 满足 n0=n2+1；形态可能不唯一，且不一定是完全二叉树。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (6, 'single', 'ds', 4, '图', 2, '某稠密图有 1000 个顶点、近 50 万条边，综合考虑存储空间和操作便利性，最适合采用的存储结构是（ ）。', '["邻接表","邻接矩阵","十字链表","邻接多重表"]', '[1]', '稠密图中邻接矩阵空间利用率高，且判断两顶点间是否有边为 O(1)；邻接表更适合边数较少的稀疏图。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (7, 'single', 'ds', 5, '排序', 2, '下列排序算法中，平均时间复杂度为 O(n log n)、而最坏时间复杂度为 O(n²) 的是（ ）。', '["堆排序","快速排序","归并排序","简单选择排序"]', '[1]', '快速排序平均性能 O(nlogn)，但当划分极不平衡（如序列已有序且取首元素为基准）时退化为 O(n²)。堆排序、归并排序最坏仍为 O(nlogn)。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (8, 'single', 'ds', 6, '查找', 2, '采用链地址法（拉链法）解决哈希冲突时，哈希表每个地址单元中通常保存的是（ ）。', '["一个单链表的头指针","一个循环队列","一个定长顺序表","一棵二叉排序树"]', '[0]', '链地址法将所有哈希地址相同的记录链在同一个单链表中，表头指针存入哈希表对应单元，处理冲突灵活且无堆积现象。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (9, 'multi', 'ds', 4, '图', 2, '下列关于最小生成树的叙述，正确的有（ ）。', '["Prim 算法从某顶点出发不断扩展顶点集合，更适合稠密图","Kruskal 算法按边权值从小到大依次选边，更适合稀疏图","同一个带权连通图的最小生成树可能不唯一","向最小生成树中加入任意一条非树边，都会形成唯一的一条回路"]', '[0,1,2,3]', '四项均正确。当存在权值相同的边时最小生成树可能不唯一；树中任意两顶点间路径唯一，加入一条非树边恰好形成一个环。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (10, 'multi', 'ds', 2, '栈和队列', 2, '下列关于栈和队列的叙述，正确的有（ ）。', '["栈和队列都是操作位置受限的线性表","循环队列通过取模运算解决了顺序队列的\\"假溢出\\"问题","链栈一般不存在栈满问题（仅受可用内存限制）","栈在递归调用、括号匹配、表达式求值中有重要应用"]', '[0,1,2,3]', '四项均为栈与队列的基本性质与典型应用，其中循环队列将存储空间逻辑上视为环形，通过模运算循环使用下标。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (11, 'judge', 'ds', 3, '树与二叉树', 2, '在任意一棵非空二叉树中，度为 0 的叶子结点数 n0 与度为 2 的结点数 n2 之间恒满足 n0 = n2 + 1。', '["正确","错误"]', '[0]', '由二叉树的边数关系 n-1=n1+2n2 与 n=n0+n1+n2 联立消元，可得 n0=n2+1。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (12, 'single', 'co', 7, '系统概述', 1, '冯·诺依曼计算机的基本工作方式可概括为（ ）。', '["多指令流多数据流处理","存储程序、顺序执行","硬件并行流水处理","微程序解释执行"]', '[1]', '冯·诺依曼思想的核心是"存储程序"：将程序和数据预先存入主存，计算机按地址顺序取出指令并自动执行。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (13, 'single', 'co', 8, '数据的表示', 2, 'n 位补码（含 1 位符号位）所能表示的带符号整数范围是（ ）。', '["-2^(n-1) ~ 2^(n-1) - 1","-(2^(n-1) - 1) ~ 2^(n-1) - 1","-2^n ~ 2^n - 1","0 ~ 2^n - 1"]', '[0]', '补码表示中 0 唯一，负数比正数多一个：最小为 -2^(n-1)（如8位的-128），最大为 2^(n-1)-1（如+127）。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (14, 'single', 'co', 9, '存储器层次结构', 2, '下列关于 SRAM 与 DRAM 的叙述，错误的是（ ）。', '["SRAM 存取速度快，常用作高速缓存 Cache","DRAM 依靠电容存储电荷，需要定期刷新","DRAM 集成度高、每位成本低，常用作主存","SRAM 需要定时刷新，而 DRAM 不需要刷新"]', '[3]', '事实恰好相反：DRAM 因电容漏电需要刷新，SRAM 由触发器保存信息，只要不断电就不会丢失，无需刷新。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (15, 'single', 'co', 9, '存储器层次结构', 2, '在 Cache 的地址映射方式中，块冲突概率最低、Cache 空间利用率最高，但比较电路复杂、硬件成本最高的是（ ）。', '["直接映射","全相联映射","组相联映射","段相联映射"]', '[1]', '全相联映射允许主存块装入 Cache 任意行，冲突最少，但需对所有行的标记同时比较，成本最高。直接映射冲突最多，组相联是折中方案。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (16, 'single', 'co', 10, '中央处理器', 1, '一个完整的指令周期可包含取指、间址、执行、中断等子周期，其中取指周期完成的工作是（ ）。', '["按程序计数器地址从主存取出指令送入指令寄存器","计算操作数的有效地址","执行指令所规定的运算操作","响应并处理中断请求"]', '[0]', '取指周期根据 PC 的内容访问主存，将指令取回 IR，同时 PC 自动加 1 指向下一条指令。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (17, 'single', 'co', 10, '中央处理器', 3, '某计算机主频为 500MHz，执行含 10000 条指令的某程序共耗时 0.1ms，则该程序执行的平均 CPI 为（ ）。', '["3","5","8","10"]', '[1]', '总时钟周期数 = 500×10^6 × 0.1×10^-3 = 5×10^4；CPI = 5×10^4 / 10^4 = 5。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (18, 'single', 'co', 10, '中央处理器', 2, '一条线性流水线共有 4 个功能段，各段时间均为 Δt。连续输入 10 条任务时，流水线的总执行时间为（ ）。', '["10Δt","13Δt","14Δt","40Δt"]', '[1]', '第一条任务需要 kΔt 流满流水线，之后每 Δt 流出一条，总时间为 (k+n-1)Δt=(4+10-1)Δt=13Δt。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (19, 'multi', 'co', 11, '输入输出系统', 2, '下列关于程序中断方式与 DMA 方式的叙述，正确的有（ ）。', '["中断方式通常以字（字节）为单位完成一次数据传送","DMA 方式以数据块为单位进行传送","DMA 传送由硬件控制器直接控制，不经过 CPU 内部寄存器中转","DMA 请求的总线优先级通常高于一般中断请求"]', '[0,1,2,3]', '四项均正确。DMA 适用于高速外设大批量数据传输，仅在传送开始和结束时需要 CPU 干预，可配合周期窃取使用总线。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (20, 'multi', 'co', 8, '数据的表示', 3, '下列关于 IEEE 754 单精度浮点数格式的叙述，正确的有（ ）。', '["总长度 32 位，包含 1 位符号位、8 位阶码和 23 位尾数字段","阶码采用移码表示，约定的偏置值为 127","规格化数的尾数隐含整数位 1，尾数实际精度为 24 位","格式中可表示正零、负零、正无穷、负无穷以及非数 NaN"]', '[0,1,2,3]', '四项均为 IEEE754 单精度格式的规定。阶码字段范围 1~254 表示规格化数，0 与 255 保留给零、非规格化数、无穷和 NaN。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (21, 'judge', 'co', 11, '输入输出系统', 2, 'DMA 方式下数据在主存与外设之间直接成批传送，总线由 DMA 控制器接管期间，CPU 仍可进行不依赖总线的内部操作。', '["正确","错误"]', '[0]', 'DMA 传送期间 CPU 暂停访问总线，但可继续执行已在指令队列中的操作（周期窃取方式下影响更小），仅在块传送结束后通过中断通知 CPU。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (22, 'single', 'os', 12, '进程管理', 1, '下列关于进程与线程的叙述，正确的是（ ）。', '["进程是 CPU 调度的基本单位，线程是资源分配的基本单位","线程是 CPU 调度的基本单位，进程是资源拥有的基本单位","同一进程内的多个线程不能并发执行","线程切换的开销通常大于进程切换"]', '[1]', '在引入线程的系统中，线程是独立调度的最小单位，进程作为资源分配的基本单位。同进程内线程共享地址空间，切换无需更换页表，开销更小。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (23, 'single', 'os', 12, '进程管理', 1, '某正在运行的进程时间片用完后，其状态将由运行态转换为（ ）。', '["阻塞态","就绪态","终止态","就绪挂起态"]', '[1]', '时间片用完属于"运行→就绪"转换；只有等待某事件（如 I/O、信号量）时才进入阻塞态。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (24, 'single', 'os', 13, '进程同步', 2, '用 P、V 操作管理互斥临界区时，互斥信号量初值一般为 1。进程执行 P 操作使信号量减 1，若结果（ ）则该进程进入阻塞队列等待。', '["大于 0","等于 0","小于 0","不等于 0"]', '[2]', 'P 操作中 S=S-1，若 S<0 表示已有进程占用临界区，当前进程必须阻塞；其绝对值表示等待该资源的进程数。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (25, 'single', 'os', 14, '死锁', 1, '下列各项中，不属于死锁产生四个必要条件的是（ ）。', '["互斥条件","请求并保持条件","可抢占条件","循环等待条件"]', '[2]', '四个必要条件为：互斥、请求并保持、不可抢占、循环等待。"可抢占"恰恰破坏了"不可抢占"条件。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (26, 'single', 'os', 14, '死锁', 1, '银行家算法通过判断系统分配资源后是否仍处于安全状态来决定是否批准请求，它属于一种（ ）算法。', '["死锁预防","死锁避免","死锁检测","死锁解除"]', '[1]', '死锁避免在分配前进行安全性检查，保证系统始终存在安全序列；银行家算法是其典型代表。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (27, 'single', 'os', 15, '内存管理', 3, '某分页存储管理系统页面大小为 4KB，某逻辑地址为十六进制 1F3A5H，则该地址的页号（十进制）为（ ）。', '["30","31","933","32"]', '[1]', '4KB=2^12=1000H，页内地址占低 12 位。1F3A5H ÷ 1000H 商为 1FH=31，余数 3A5H 为页内偏移。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (28, 'single', 'os', 15, '内存管理', 3, '系统为某进程分配 3 个物理块（初始为空），页面访问序列为 1,2,3,1,4,2。采用 LRU 页面置换算法，发生缺页的次数为（ ）。', '["4 次","5 次","6 次","3 次"]', '[1]', '前 3 次均缺页；第4次访问1命中；访问4时淘汰最久未用的2（缺页）；再访问2时淘汰最久未用的3（缺页），共 5 次缺页。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (29, 'multi', 'os', 15, '内存管理', 2, '虚拟存储器技术得以实现的基础包括（ ）。', '["程序执行的时间局部性原理","程序执行的空间局部性原理","用于存放暂不运行部分的大容量辅助存储器","一定容量的物理主存与动态地址转换机构"]', '[0,1,2,3]', '局部性原理保证进程只需部分装入内存即可运行；辅存作为内存的"后援"，再配合请求调页/调段和缺页中断共同构成虚拟存储系统。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (30, 'multi', 'os', 16, '设备管理', 2, '下列磁盘调度算法中，可能使某些请求长时间得不到服务（"饥饿"现象）的是（ ）。', '["FCFS 先来先服务","SSTF 最短寻道时间优先","SCAN 电梯算法","C-SCAN 循环扫描算法"]', '[1]', 'SSTF 总是优先服务距离当前磁头最近的请求，若新请求不断到来且更近，远端请求可能无限等待。SCAN/C-SCAN 沿固定方向扫描，等待时间有上界。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (31, 'judge', 'os', 17, '文件管理', 2, '采用多级（树形）目录结构能够有效解决不同用户之间文件重名的问题。', '["正确","错误"]', '[0]', '文件由其路径唯一标识，不同目录下可以存在同名文件；多级目录还便于分类管理、权限控制和文件共享。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (32, 'single', 'cn', 18, '体系结构', 1, '在 OSI 参考模型中，负责在相邻结点之间进行成帧、差错控制与流量控制的层次是（ ）。', '["物理层","数据链路层","网络层","传输层"]', '[1]', '数据链路层在相邻结点（点到点）之间提供帧传输与差错控制；传输层提供的是端到端（进程到进程）的可靠通信。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (33, 'single', 'cn', 19, '物理层', 3, '根据香农公式，带宽为 3kHz、信噪比为 30dB 的无离散噪声信道，其极限信息传输速率约为（ ）。', '["3 kbps","30 kbps","300 kbps","60 kbps"]', '[1]', '30dB 对应 S/N=10^3=1000；C=W log2(1+S/N)=3000×log2(1001)≈3000×9.97≈29.9kbps，约 30kbps。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (34, 'single', 'cn', 20, '数据链路层', 2, '传统以太网规定最小帧长为 64 字节，其主要目的是（ ）。', '["提高信道利用率","保证站点在帧发送完毕之前能够检测到可能发生的冲突","简化 CRC 校验计算","限制单帧占用信道的最长时间"]', '[1]', 'CSMA/CD 要求冲突在发送期间被发现。最短帧长 = 2τ 内可发送的比特数（争用期），64 字节由此而来，过短的帧将被视为冲突碎片。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (35, 'single', 'cn', 21, '网络层', 2, '主机 IP 地址为 192.168.1.130，子网掩码为 255.255.255.192，该地址所在子网的网络地址是（ ）。', '["192.168.1.0","192.168.1.64","192.168.1.128","192.168.1.192"]', '[2]', '130 的二进制为 10000010，与掩码末字节 11000000 按位与得到 10000000=128，故网络地址为 192.168.1.128/26。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (36, 'single', 'cn', 21, '网络层', 1, 'ARP 协议的主要功能是（ ）。', '["将域名解析为 IP 地址","将同一局域网内主机的 IP 地址解析为 MAC 物理地址","将 MAC 地址解析为 IP 地址","为主机自动分配 IP 地址"]', '[1]', 'ARP（地址解析协议）通过广播请求、单播应答获取目的 IP 对应的 MAC；A 是 DNS 的功能，C 是 RARP，D 是 DHCP。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (37, 'single', 'cn', 22, '传输层', 1, 'TCP 建立连接采用三次握手，其报文段标志位的正确顺序是（ ）。', '["SYN → SYN+ACK → ACK","SYN → ACK → SYN","SYN+ACK → SYN → ACK","SYN → FIN → ACK"]', '[0]', '客户端发 SYN，服务器回复 SYN+ACK，客户端再发 ACK，连接建立。三次握手可同步双方初始序列号并防止失效连接请求造成混乱。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (38, 'single', 'cn', 22, '传输层', 2, 'TCP 拥塞控制的慢开始阶段，拥塞窗口 cwnd 的变化规律是（ ）。', '["每经过一个往返 RTT 增加 1 个 MSS","每经过一个往返 RTT 增大一倍（指数增长）","在整个通信期间保持固定不变","随网络拥塞程度线性减小"]', '[1]', '慢开始阶段 cwnd 从 1 个 MSS 起步，每收到一轮全部确认就加倍（1→2→4→8），达到慢开始门限 ssthresh 后转入拥塞避免的线性增长。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (39, 'multi', 'cn', 22, '传输层', 2, '下列关于 TCP 与 UDP 的比较，正确的有（ ）。', '["TCP 面向连接，UDP 在传输数据前无需建立连接","TCP 提供可靠交付服务，UDP 不保证数据可靠到达","TCP 首部至少 20 字节，UDP 首部固定 8 字节，TCP 开销更大","UDP 支持一对一、一对多、多对多通信，TCP 连接只能点对点"]', '[0,1,2,3]', '四项均正确。UDP 结构简单、时延小，适合实时语音、视频、DNS 等场景；TCP 通过确认、重传、序号等机制保证可靠字节流。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (40, 'multi', 'cn', 23, '应用层', 1, '下列应用层协议与其默认服务端口的对应关系，正确的有（ ）。', '["HTTP — 80","HTTPS — 443","DNS — 53","FTP 控制连接 — 21"]', '[0,1,2,3]', '以上均为 IANA 规定的知名端口。此外 FTP 数据连接默认端口 20，SMTP 为 25，SSH 为 22。', '内置题库', 0, 0);
INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES (41, 'judge', 'cn', 22, '传输层', 2, 'TCP 的滑动窗口机制可实现流量控制；流量控制是收发双方之间的端到端控制，而拥塞控制需要考虑整个网络的承载能力。', '["正确","错误"]', '[0]', '接收方通过通告窗口 rwnd 限制发送速率实现流量控制；发送方还依据网络状况维护拥塞窗口 cwnd，二者取较小值决定实际发送窗口。', '内置题库', 0, 0);
