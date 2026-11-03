# wxxcx

一个「H5 先行、再转小程序」的个人项目骨架：

- `frontend/` uni-app（Vue 3 + Vite + TypeScript），H5 与微信小程序共用一套代码
- `backend/` Spring Boot 4 + Java 17，JDBC + Flyway
- `Dockerfile` 位于仓库根目录，供微信云托管用 Git 构建
- `.github/workflows/build.yml` 自动构建后端 + 构建小程序并上传体验版

用户身份统一走微信 `openid`：小程序经云托管 `callContainer` 调用时，云托管自动注入 `X-WX-OPENID` 请求头，后端直接读，不需要 appsecret，也不需要手机号。

## 环境要求

| 依赖 | 说明 |
|------|------|
| JDK 17 | `brew install --cask temurin@17`，Spring Boot 4 不支持 JDK 8/11 |
| Node 20+ | 前端构建 |
| 微信开发者工具 | 预览小程序 |
| MySQL 8 | 本地或云上均可 |

`frontend/.npmrc` 里把 registry 固定成了公共 npm 源：`@dcloudio/*` 的最新版本在内网镜像上不同步，用公司源会装不上。

## 一、数据库

在云 MySQL（或本地 MySQL 8）里建库和账号：

```sql
CREATE DATABASE IF NOT EXISTS wxxcx DEFAULT CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'wxxcx'@'%' IDENTIFIED BY '换成自己的密码';
GRANT ALL PRIVILEGES ON wxxcx.* TO 'wxxcx'@'%';
```

两个容易踩的坑：

- `CREATE USER IF NOT EXISTS` **不会**改已存在账号的密码。如果这个账号之前建过，必须再执行 `ALTER USER 'wxxcx'@'%' IDENTIFIED BY '新密码';`，否则后端会一直报 `Access denied ... (using password: YES)`。
- `FLUSH PRIVILEGES` 在 CREATE USER / GRANT 之后是不需要的；用它反而可能因为没有 `RELOAD` 权限报 `ERROR 1227`。

改完密码用同一条链路验一遍，别只看建账号是否成功：

```bash
MYSQL_PWD='你的密码' mysql -h 外网地址 -P 端口 -u wxxcx -e 'select current_user(), database();' wxxcx
```

表结构不用手工建，后端启动时 Flyway 会跑 `backend/src/main/resources/db/migration/`。

## 二、本地开发

本地调试是「两个终端」：终端 A 跑后端，终端 B 跑 H5。浏览器里点页面 → vite 代理 → 本地 Spring Boot → 云 MySQL，跟线上只差一个 openid 的来源。

### 终端 A：后端

先把数据库配置放到 `backend/.env.local`（已被 `.gitignore` 忽略）：

```bash
cd backend
cp .env.local.example .env.local      # 然后编辑，填外网地址:外网端口 和密码
./run-local.sh                        # 自动读取 .env.local 并启动，默认 8080
```

`run-local.sh` 会在缺配置时直接报错退出，也会自动找 JDK 17；不用每次重敲一长串环境变量。

手动起等价于：

```bash
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # 只设 JAVA_HOME 不会改变 java -version，PATH 也要带上
export PATH="$JAVA_HOME/bin:$PATH"

MYSQL_ADDRESS=外网地址:外网端口 MYSQL_DATABASE=wxxcx MYSQL_USERNAME=wxxcx MYSQL_PASSWORD=密码 \
./mvnw spring-boot:run
```

注意环境变量只对**当次命令**生效。少写任意一项都会回落到 `application.yml` 的默认值（`127.0.0.1:3306` / `root` / 空密码），症状是 `Connection refused`。

启动成功的标志是日志里出现 Flyway 的 `Successfully applied ... migration` 和 Tomcat 的 `Tomcat started on port 8080`。

先自检：

```bash
curl -s localhost:8080/api/health          # {"status":"ok"}
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/api/me   # 401，没有 openid
curl -s -H 'X-WX-OPENID: dev-openid' localhost:8080/api/me       # 200，会顺手建一行 app_user
```

> 云 MySQL 的内网地址只能云端访问，本地调试要用「外网地址 + 外网端口」，并确认白名单里有你当前出口 IP。调试完记得关掉外网访问。

### 终端 B：前端 H5

```bash
cd frontend
cp .env.example .env      # 可以留空，H5 走 vite 代理
npm install
npm run dev:h5
```

打开 http://localhost:5173 。vite 代理把 `/api` 转发到 `http://127.0.0.1:8080`，并补一个假的 `X-WX-OPENID: dev-openid`，所以浏览器里也有"当前用户"。

换测试身份（每个 openid 就是一个不同账号，用来验证"未登记会跳注册页""同一个人只看到自己的数据"）：直接在浏览器控制台执行，然后刷新页面，不用重启服务。

```js
document.cookie = 'dev_openid=tom; path=/'        // 切成 tom（没登记过，会跳注册页）
document.cookie = 'dev_openid=dev-openid; path=/' // 切回默认身份
```

代理会优先用这个 cookie 覆盖 `X-WX-OPENID`；没有 cookie 时退回 `DEV_OPENID` 环境变量（默认 `dev-openid`）。

换后端端口：

```bash
API_TARGET=http://127.0.0.1:8081 npm run dev:h5
```

### 改代码之后

| 改了什么 | 怎么生效 |
|----------|----------|
| 前端 `.vue` / `.ts` | vite HMR，保存即刷新页面 |
| 后端 `.java` | `spring-boot-devtools` 已引入；`./mvnw spring-boot:run` 是前台进程，改完在**另一个终端**执行 `cd backend && ./mvnw -q compile`，classpath 一变容器就自动重启 |
| `db/migration/*.sql` | Flyway 只在启动时跑。新增 `V3__xxx.sql` 重启即可；**已执行过的迁移不要改**，checksum 变了会启动失败 |
| `backend/pom.xml` | 停掉后端，重新 `./mvnw spring-boot:run` |

### 调试注册流程（当前业务）

1. 浏览器打开 http://localhost:5173 ，首页应显示 openid 卡片和「填写收货信息」按钮。
2. 点进注册页，填省 / 市 / 小区 / 栋 / 单元 / 房号，提交。
3. 首页列表出现这条地址（回到首页会重新拉一次 `GET /api/addresses`）。
4. 直接查库确认落库的 openid 是当前身份：

   ```sql
   SELECT id, openid, province, city, community, building, unit, room_no, created_at
   FROM user_address ORDER BY id DESC LIMIT 5;
   ```

5. 想清空重来：`DELETE FROM user_address WHERE openid = 'dev-openid';`

后端有异常时，浏览器 F12 → Network 里看 `/api/addresses` 的响应体，比 toast 里的信息全；后端终端里同时会有堆栈。

### 小程序端怎么调

`wx.cloud.callContainer` 走的是云托管通道，不是普通 HTTP，本地起 8080 是调不通的。所以策略是：

- UI、表单、接口逻辑、数据落库都在 H5 里调通（这一步能覆盖绝大部分改动）；
- 小程序只在需要验证平台差异时跑：`npm run dev:mp-weixin` → 用微信开发者工具打开 `frontend/dist/dev/mp-weixin`，此时它调用的是**已部署的云托管服务**，也就是要先把后端部署上去；
- 云托管控制台提供「本地调试」隧道，可以把工具里的 `callContainer` 打到本机，但要求云托管服务和本地代码版本一致，日常改 UI 用不上。

首要注意的差异只有一条：H5 的身份来自 vite 代理注入的假 openid，小程序的来自云托管真实注入，所以**线上的数据不要用 `dev-openid` 那条去对照**。

## 三、部署后端到微信云托管

1. 云托管控制台 → 新建服务（例如 `java-service`）→ 部署方式选「Git 仓库」，绑定 `fyld5408/wxxcx` 的 `main` 分支。
2. 构建配置：Dockerfile 路径填 `Dockerfile`（仓库根目录），监听端口填 `80`。
3. 服务环境变量：

   | 变量 | 值 |
   |------|----|
   | `MYSQL_ADDRESS` | MySQL 内网地址 + 端口，如 `10.0.x.x:3306` |
   | `MYSQL_DATABASE` | `wxxcx` |
   | `MYSQL_USERNAME` | `wxxcx` |
   | `MYSQL_PASSWORD` | 对应密码 |

4. 健康检查路径填 `/api/health`。
5. 之后每次 `git push` 到 `main`，云托管自动重新构建部署。

> 用外网地址跑通了不代表云端能连，云端必须用**内网地址**，而且云托管服务和 MySQL 要在同一环境。

## 四、小程序端

1. 微信公众平台建小程序，拿到 AppID，填进 `frontend/src/manifest.json` 的 `mp-weixin.appid`。
2. 复制 `.env.example` 为 `.env`，填：

   ```
   VITE_MP_CLOUD_ENV=云托管环境ID
   VITE_MP_SERVICE_NAME=java-service
   ```

3. 构建后在微信开发者工具里打开产物：

   ```bash
   npm run dev:mp-weixin
   # 打开 frontend/dist/dev/mp-weixin
   ```

   `callContainer` 不经过普通网络请求，**不需要配 request 合法域名，也不需要域名备案**；正式发布仍然需要小程序备案。

## 五、CI 自动上传体验版

在 GitHub 仓库 Secrets 里配置：

| Secret | 说明 |
|--------|------|
| `MP_APPID` | 小程序 AppID |
| `MP_PRIVATE_KEY` | 微信公众平台「开发管理 → 开发设置 → 小程序代码上传密钥」的完整内容 |
| `MP_CLOUD_ENV` | 云托管环境 ID |
| `MP_SERVICE_NAME` | 云托管服务名，如 `java-service` |

推 `main` 后，workflow 会构建后端、构建小程序并上传一个体验版（版本号用 run number）。密钥只存在 Secret 里，不要提交到仓库。

## 六、接自己的业务

- 后端加接口：在 `com.fyld.wxxcx` 下按业务建包，Controller 里用 `@RequestHeader("X-WX-OPENID")` 拿当前用户。
- 加表：往 `db/migration` 加下一个序号的迁移（现在是 `V4__xxx.sql`），不要改已经执行过的 V1 / V2 / V3。
- 前端加页面：`src/pages.json` 注册，`src/utils/api.ts` 里复用 `request()`。

## 七、当前接口和页面

| 接口 | 说明 |
|------|------|
| `GET /api/address` | 当前用户的登记信息；未登记返回 204 |
| `POST /api/address` | 首次登记；同一 openid 重复提交返回 409 |
| `POST /api/votes` | `{title, choice}`，choice 取 `SUPPORT` / `OPPOSE`；未登记返回 409 |
| `POST /api/topics/{id}/votes` | `{choice}`，对已有主题投票（列表页「我要投票」走这个） |
| `GET /api/topics` | 主题列表，带支持 / 不支持票数 |
| `GET /api/topics/{id}` | 主题详情 + 投票明细 + 自己投的那一票 |

页面：首页（未登记会被 `reLaunch` 到注册页）→ 发起投票 → 投票结果列表 → 投票明细。

两条唯一约束是数据模型的核心：`user_address.openid` 唯一（一个人只有一条门牌），`vote_record(topic_id, openid)` 唯一（一个人对同一主题只有一行，重复投票覆盖原记录）。主题按 `vote_topic.title` 唯一，同名主题自动复用。
