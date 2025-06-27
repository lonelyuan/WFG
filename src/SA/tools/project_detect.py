from pathlib import Path

# 特征定义：文件名/目录名 => 语言/框架的映射
SIGNATURES = {
    # Python
    'requirements.txt': ('Python', 'pip'),
    'pyproject.toml': ('Python', 'poetry or PEP 517'),
    'Pipfile': ('Python', 'pipenv'),
    'manage.py': ('Python', 'Django'),
    'setup.py': ('Python', 'setuptools'),
    'main.py': ('Python', None),
    '__init__.py': ('Python', None),
    
    # Node.js / JavaScript / TypeScript
    'package.json': ('JavaScript/TypeScript', 'Node.js'),
    'vite.config.js': ('JavaScript/TypeScript', 'Vite'),
    'next.config.js': ('JavaScript/TypeScript', 'Next.js'),
    'tsconfig.json': ('TypeScript', None),

    # Java
    'pom.xml': ('Java', 'Maven'),
    'build.gradle': ('Java', 'Gradle'),
    'settings.gradle': ('Java', 'Gradle'),

    # Rust
    'Cargo.toml': ('Rust', 'Cargo'),

    # Go
    'go.mod': ('Go', 'Modules'),

    # C/C++
    'CMakeLists.txt': ('C/C++', 'CMake'),
    'Makefile': ('C/C++', 'Make'),

    # PHP
    'composer.json': ('PHP', 'Composer'),
    'artisan': ('PHP', 'Laravel'),

    # Ruby
    'Gemfile': ('Ruby', 'Bundler'),
    'Rakefile': ('Ruby', 'Rake'),

    # Python notebooks
    '.ipynb': ('Python', 'Jupyter'),

    # Docker / DevOps
    'Dockerfile': ('DevOps', 'Docker'),
    'docker-compose.yml': ('DevOps', 'Docker Compose'),
    '.github': ('DevOps', 'GitHub Actions'),
    '.gitlab-ci.yml': ('DevOps', 'GitLab CI'),
}

def detect_project_type(base_path='.'):
    base = Path(base_path)
    detected = {}

    for path in base.rglob('*'):
        name = path.name

        if name in SIGNATURES:
            lang, tool = SIGNATURES[name]
            detected.setdefault(lang, set()).add(tool)

        if path.is_file() and path.suffix in SIGNATURES:
            lang, tool = SIGNATURES[path.suffix]
            detected.setdefault(lang, set()).add(tool)

    return detected

if __name__ == "__main__":
    result = detect_project_type('.')
    print(result)
    for lang, tools in result.items():
        tools_clean = ', '.join([t for t in tools if t])
        print(f"Detected {lang} project", end='')
        if tools_clean:
            print(f" using: {tools_clean}")
        else:
            print()