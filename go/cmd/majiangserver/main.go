// 麻将算法与网页对战平台主入口(Go 版),对应 Java web.Main。
package main

import (
	"flag"
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"

	majiang "github.com/esrrhs/majiang_algorithm/go"
	"github.com/esrrhs/majiang_algorithm/go/web"
)

func main() {
	port := flag.Int("port", 8080, "HTTP 监听端口")
	cli := flag.Bool("cli", false, "运行 4 个 AI 自动对战模拟 (CLI Benchmark)")
	flag.Parse()

	// 兼容 Java 风格的 --port=8080 参数
	for _, arg := range os.Args[1:] {
		if strings.HasPrefix(arg, "--port=") {
			if p, err := strconv.Atoi(arg[len("--port="):]); err == nil {
				*port = p
			} else {
				fmt.Fprintf(os.Stderr, "无效端口参数，使用默认端口: %d\n", *port)
			}
		}
	}

	fmt.Println("==================================================")
	fmt.Println("   麻将算法 & AI 网页对战平台 (Majiang Algorithm)   ")
	fmt.Println("==================================================")
	fmt.Println("正在预加载高性能胡牌与 AI 查表数据，请稍候...")

	start := time.Now()
	if err := majiang.HuLoad(); err != nil {
		fmt.Fprintf(os.Stderr, "胡牌查表加载失败: %v\n", err)
		os.Exit(1)
	}
	huLoaded := time.Now()
	fmt.Printf("胡牌查表 (HuTable) 加载完成，耗时: %d ms\n", huLoaded.Sub(start).Milliseconds())

	if err := majiang.AILoad(); err != nil {
		fmt.Fprintf(os.Stderr, "AI 查表加载失败: %v\n", err)
		os.Exit(1)
	}
	aiLoaded := time.Now()
	fmt.Printf("AI 查表 (AITable) 加载完成，耗时: %d ms\n", aiLoaded.Sub(huLoaded).Milliseconds())
	fmt.Printf("全套查表加载完毕，累计耗时: %d ms\n", aiLoaded.Sub(start).Milliseconds())
	fmt.Println("--------------------------------------------------")

	if *cli {
		web.RunCliSimulation()
		return
	}

	server := web.NewServer(*port)
	if err := server.Start(); err != nil {
		fmt.Fprintf(os.Stderr, "启动服务器失败: %v\n", err)
		os.Exit(1)
	}
}
