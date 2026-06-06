裁判文书标注平台前端 MVP

## 技术栈

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router

## 启动

```bash
npm install
npm run dev
```

默认地址：`http://localhost:5173`

Vite 已配置 `/api` 代理到 `http://localhost:8080`，请先启动后端。


## 前端打包

```bash
npm run build
```

## 连接服务器
```angular2html
ssh -p22 ubuntu@129.28.41.192

cd judgment-platform/
```

**重启前后端**
```angular2html
sudo docker compose restart backend frontend
```

## 访问地址
```angular2html
http://129.28.41.192:8086/login
```

## 查看数据库
```angular2html
sudo docker exec -it judgment-mysql mysql -uroot -p

```

2. 停止并删除当前 MySQL 容器及数据卷
   ```bash
   sudo docker compose down -v 
   ```
    
   这一步会删除容器、网络和名为 judgment-mysql-data 的数据卷，彻底清除旧数据。

3. 重新启动服务
   ```bash
   sudo docker compose up -d
   ```
   MySQL 容器第一次启动时，会执行 ./init.sql 初始化数据库，新的表结构就会生效。
4. 重启数据库
   ```bash
   sudo docker compose -p judgment restart mysql
   ```