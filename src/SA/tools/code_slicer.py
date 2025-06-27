def slice_code(project_path: str, codeslice: str) -> str:
    """
    输入代码路径和行号范围，返回源代码字符串

    Args:
        project_path: 根路径
        codeslice: 代码切片字符串，格式为："path:L<start_line>-L<end_line>"

    Returns:
        code: 代码字符串
    """
    path, line_range = codeslice.split(":")
    start_line, end_line = line_range.split("-")
    start_line = int(start_line[1:])
    end_line = int(end_line[1:])
    if start_line > end_line or start_line < 0 or end_line < 0:
        raise ValueError
    from pathlib import Path
    path = Path(project_path) / path
    with open(path, "r", encoding="utf-8") as f:
        lines = f.readlines()
        return "".join(lines[start_line:end_line])



