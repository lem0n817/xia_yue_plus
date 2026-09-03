"""越权测试服务：/api/user 无论是否携带认证都返回手机号（模拟水平越权/未授权访问漏洞）。
无论客户端是否要求压缩，始终返回 gzip 响应（模拟强制压缩的服务器，验证插件解压路径）。"""
import gzip as gzip_mod
import http.server
import json

BODY = json.dumps({"code": 0, "msg": "ok", "data": {"name": "张三", "mobile": "13812345678", "email": "zhangsan@test.local"}}, ensure_ascii=False).encode("utf-8")
GZ = gzip_mod.compress(BODY)


class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        auth = self.headers.get("Cookie")
        enc = self.headers.get("Accept-Encoding")
        print(f"[srv] GET {self.path} cookie={auth!r} ae={enc!r}", flush=True)
        self.send_response(200)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Encoding", "gzip")
        self.send_header("Content-Length", str(len(GZ)))
        self.end_headers()
        self.wfile.write(GZ)

    def log_message(self, *a):
        pass


if __name__ == "__main__":
    print("[srv] listening on 127.0.0.1:9999", flush=True)
    http.server.ThreadingHTTPServer(("127.0.0.1", 9999), Handler).serve_forever()
