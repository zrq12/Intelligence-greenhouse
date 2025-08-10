import textwrap

# 获取用户输入的值
text = "广东省深圳市南山区西丽街道和计划·心灵工坊万科云设计公社B区"

# 指定每行的宽度
width = 9
# 调用wrap方法进行自动换行
wrapped_text = textwrap.wrap(text, width)

# 打印自动换行后的结果
for line in wrapped_text:
    print(line)