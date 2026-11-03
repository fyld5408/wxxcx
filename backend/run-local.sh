#!/usr/bin/env bash
# 本地调试启动脚本：读取同目录 .env.local 里的数据库配置后启动后端。
# .env.local 已被 .gitignore 忽略，不要提交。
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env.local ]; then
  echo "缺少 backend/.env.local" >&2
  echo "先执行：cp .env.local.example .env.local  然后填好里面的值" >&2
  exit 1
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

set -a
. ./.env.local
set +a

echo "连接 ${MYSQL_USERNAME}@${MYSQL_ADDRESS}/${MYSQL_DATABASE}"
exec ./mvnw spring-boot:run
