#!/usr/bin/env python3
"""Independent loopback effect store; stable IDs avoid duplicate fixture effects."""
import argparse,json,sqlite3,threading
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
p=argparse.ArgumentParser();p.add_argument('--port',type=int,required=True);p.add_argument('--database',required=True);a=p.parse_args()
lock=threading.Lock()
with sqlite3.connect(a.database) as db:
 db.execute('CREATE TABLE IF NOT EXISTS effects(id TEXT PRIMARY KEY,payload TEXT NOT NULL)')
class Handler(BaseHTTPRequestHandler):
 def log_message(self,*args): pass
 def do_POST(self):
  if self.path!='/effects':self.send_error(404);return
  body=json.loads(self.rfile.read(int(self.headers['Content-Length'])))
  with lock,sqlite3.connect(a.database) as db:
   db.execute('INSERT OR IGNORE INTO effects VALUES(?,?)',(body['externalOperationId'],json.dumps(body,sort_keys=True)))
  # Drop response AFTER independent effect commit to force UNKNOWN_EXTERNAL.
  if body.get('payload',{}).get('dropResponse'):
   self.connection.close();return
  self.send_response(200);self.end_headers();self.wfile.write(b'{"result":"CONFIRMED_SUCCESS"}')
 def do_GET(self):
  with lock,sqlite3.connect(a.database) as db:
   row=db.execute('SELECT payload FROM effects WHERE id=?',(self.path.removeprefix('/effects/'),)).fetchone()
  self.send_response(200 if row else 404);self.end_headers()
  if row:self.wfile.write(row[0].encode())
ThreadingHTTPServer(('127.0.0.1',a.port),Handler).serve_forever()
