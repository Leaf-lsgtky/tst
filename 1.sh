#!/system/bin/sh
#
# 最终版脚本 (排除隐藏文件/目录): generate_no_hidden_tree.sh
# 目标: 仅使用空格缩进显示目录结构和文件内容，排除所有以点开头的隐藏文件和目录。

OUTPUT_FILE="no_hidden_indent_tree.txt"
START_DIR="."
INDENT_UNIT="    " # 定义一个缩进单位，例如 4 个空格

# 1. 启动并清理旧的输出文件
echo "### 📂 目录结构与文件内容报告 (排除隐藏文件/目录) ###" > "$OUTPUT_FILE"
echo "---" >> "$OUTPUT_FILE"
echo "开始扫描目录: $START_DIR" >> "$OUTPUT_FILE"
echo "---" >> "$OUTPUT_FILE"

# 定义主要的递归函数
# 参数: $1=当前路径, $2=当前层级的缩进前缀
print_tree() {
    local path="$1"
    local prefix="$2"
    local items=""
    
    # 核心修正：使用 find 配合 -maxdepth 1 和 -mindepth 1
    # 并使用 -not -name '.*' 排除所有以点开头的隐藏文件和目录。
    items=$(find "$path" -maxdepth 1 -mindepth 1 -not -name '.*' -print 2>/dev/null | sort)

    # 兼容性检查：如果 find 不支持 -not -name '.*'，则回退到 ls 模式并使用 grep 过滤
    if [ -z "$items" ]; then
        # ls -A 包含非 . 和 .. 的隐藏文件，这里我们使用 ls -1 (每行一个) 配合 grep 过滤
        # 注意：Android Shell 对通配符支持可能不完美，这里假设通配符能正常工作
        # 如果 find 失败，我们尝试 ls + grep -v '^.*/\.' (这在 shell 中很难保证跨平台正确性)
        # 最兼容的方式是依赖 find -not -name，如果失败，则需要用户环境支持
        
        # 简化处理：如果 find 不支持排除，我们暂时依赖上一个版本的 find 结果，并在循环内跳过。
        # 但为了从源头排除，我们假设 find -not -name 在现代 Android shell 中可用。
        
        # 如果 find 结果为空，可能是 find 命令本身不支持，我们尝试 glob 模式：
        items=$(ls -1 "$path" | grep -v '^\.' | awk '{print "'"$path"'/" $0}')
    fi

    
    # 迭代项目列表
    echo "$items" | while IFS= read -r item; do
        local base_item=$(basename "$item")
        
        # 确保不会处理脚本本身在当前目录下的情况
        if [ "$base_item" = "$(basename "$0")" ]; then
            continue
        fi

        # 确定下一层级目录的缩进
        local next_prefix="${prefix}${INDENT_UNIT}"

        # 写入目录/文件名 (只用当前层级缩进)
        echo "${prefix}${base_item}" >> "$OUTPUT_FILE"

        if [ -d "$item" ]; then
            # 如果是目录，则递归调用
            print_tree "$item" "$next_prefix"
        elif [ -f "$item" ]; then
            # 如果是文件，判断是否为文本文件
            
            file_type=$(file -b "$item" 2>/dev/null)
            
            # 简单的文本文件判断逻辑
            if echo "$file_type" | grep -q "text" || \
               (echo "$file_type" | grep -qv "executable" && \
                echo "$file_type" | grep -qv "binary" && \
                echo "$file_type" | grep -qv "zip" && \
                echo "$file_type" | grep -qv "data"); then
                
                # 直接打印文件内容，前缀使用下一层级缩进
                while IFS= read -r line; do
                    echo "${next_prefix}${line}" >> "$OUTPUT_FILE"
                done < "$item"
                
                # 文件内容结束后，添加一个空行作为分隔
                echo "" >> "$OUTPUT_FILE"
            
            # 二进制文件被静默跳过
            else
                : 
            fi
        fi
        
    done
}

# 5. 执行函数
# 仅显示起始目录名，不加缩进
echo "$START_DIR" >> "$OUTPUT_FILE"
print_tree "$START_DIR" ""

# 6. 结束提示
echo "---" >> "$OUTPUT_FILE"
echo "✅ 报告已完成并保存到: $(pwd)/$OUTPUT_FILE"
