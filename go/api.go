package majiang

import "sync"

var loadMu sync.Mutex

// Load 加载全部查表文件(判胡表 + AI 表),等价 Java HuUtil.load() + AIUtil.load()。
// 表文件优先从当前目录查找,其次 data/、../data/,与 Java 侧一致。
func Load() error {
	loadMu.Lock()
	defer loadMu.Unlock()
	if err := HuLoad(); err != nil {
		return err
	}
	return AILoad()
}

// HuGen 重新生成判胡查表文件(majiang_clien_*.txt、majiang_server_*.txt),
// 写入当前目录。等价 Java HuUtil.gen(),但不含 sqlite 的 majiang.db 输出。
func HuGen() error {
	loadMu.Lock()
	defer loadMu.Unlock()
	return genHu()
}

// AiGen 重新生成 AI 查表文件(majiang_ai_*.txt),写入当前目录。等价 Java AIUtil.gen()。
func AiGen() error {
	loadMu.Lock()
	defer loadMu.Unlock()
	return genAi()
}
