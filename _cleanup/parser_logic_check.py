# Порт логики Kotlin-парсеров на Python для проверки ожиданий unit-тестов.
import base64, json, urllib.parse as up

def decode(s):
    try: return up.unquote(s)
    except Exception: return s

def qp(params, key, default=""):
    v = params.get(key)
    return v if v else default

def qpb(params, key):
    return qp(params, key).lower() in ("1","true","yes","on")

def query_params(raw):
    out = {}
    for pair in (raw or "").split('&'):
        if not pair: continue
        k, _, v = pair.partition('=')
        if k in out: continue
        out[decode(k)] = decode(v)
    return out

def transport(network, params, host, sni):
    network = (network or "tcp").lower()
    if network in ("tcp","raw","none",""): return None
    if network == "ws":
        t = {"type":"ws","path": qp(params,"path") or "/"}
        if host: t["headers"] = {"Host": host}
        return t
    if network == "grpc":
        return {"type":"grpc","service_name": qp(params,"serviceName")}
    if network == "httpupgrade":
        t = {"type":"httpupgrade","path": qp(params,"path") or "/"}
        if host: t["host"] = host
        return t
    if network in ("h2","http"):
        t = {"type":"http","path": qp(params,"path") or "/"}
        if host: t["host"] = [host]
        return t
    return None

def tls_block(params, host, default_alpn=()):
    t = {"enabled": True, "server_name": qp(params,"sni") or host,
         "insecure": qpb(params,"allowInsecure") or qpb(params,"insecure") or qpb(params,"allow_insecure")}
    alpn = [x.strip() for x in (qp(params,"alpn") or "").split(",") if x.strip()] or list(default_alpn)
    if alpn: t["alpn"] = alpn
    fp = qp(params,"fp")
    if fp: t["utls"] = {"enabled": True, "fingerprint": fp}
    return t

# --- vless ---
def parse_vless(uri):
    p = up.urlparse(uri); q = query_params(p.query)
    if not p.hostname or not p.port or not p.username: return None
    security = qp(q,"security","none"); network = qp(q,"type","tcp").lower()
    sni = qp(q,"sni") or qp(q,"peer") or p.hostname
    ob = {"type":"vless","tag":"proxy","server":p.hostname,"server_port":p.port,"uuid":decode(p.username)}
    if qp(q,"flow"): ob["flow"] = qp(q,"flow")
    if security in ("tls","reality"):
        tls = tls_block(q, p.hostname)
        if security == "reality":
            tls["reality"] = {"enabled":True,"public_key":qp(q,"pbk"),"short_id":qp(q,"sid")}
            tls["utls"] = {"enabled":True,"fingerprint": qp(q,"fp") or "chrome"}
        ob["tls"] = tls
    tr = transport(network, q, qp(q,"host") or sni, sni)
    if tr: ob["transport"] = tr
    return {"outbounds":[ob]}

# --- vmess ---
def parse_vmess(uri):
    raw = uri.strip()[8:].split('#')[0]
    try: d = json.loads(base64.b64decode(raw + "="*(-len(raw)%4)).decode())
    except Exception: return None
    host = d.get("add"); uid = d.get("id")
    if not host or not uid: return None
    ob = {"type":"vmess","tag":"proxy","server":host,"server_port":int(d.get("port",443)),
          "uuid":uid,"alter_id":int(d.get("aid",0) or 0),"security":d.get("scy") or "auto"}
    if d.get("tls") == "tls":
        tls = {"enabled":True,"server_name":d.get("sni") or d.get("host") or host,"insecure":False}
        if d.get("alpn"): tls["alpn"]=[x.strip() for x in str(d["alpn"]).split(",") if x.strip()]
        if d.get("fp"): tls["utls"]={"enabled":True,"fingerprint":d["fp"]}
        ob["tls"]=tls
    net = (d.get("net") or "tcp").lower()
    tr = transport(net, {}, d.get("host") or d.get("sni") or host, d.get("path",""))
    if tr: ob["transport"]=tr
    return {"outbounds":[ob]}

# --- trojan ---
def parse_trojan(uri):
    p = up.urlparse(uri); q = query_params(p.query)
    if not p.hostname or not p.port or not p.username: return None
    ob = {"type":"trojan","tag":"proxy","server":p.hostname,"server_port":p.port,"password":decode(p.username)}
    if qp(q,"security","tls") != "none": ob["tls"] = tls_block(q, p.hostname)
    sni = qp(q,"sni") or p.hostname
    tr = transport(qp(q,"type","tcp"), q, qp(q,"host") or sni, sni)
    if tr: ob["transport"]=tr
    return {"outbounds":[ob]}

# --- ss ---
def parse_ss(uri):
    body = uri.strip()[5:].split('#')[0]
    if not body: return None
    if '@' not in body.split('?')[0]:
        try:
            dec = base64.b64decode(body + "="*(-len(body)%4)).decode()
            if '@' in dec: body = dec
        except Exception: pass
    query = body.partition('?')[2]
    if 'plugin' in query_params(query): return None
    bare = body.split('?')[0]
    at = bare.rfind('@')
    if at > 0:
        userinfo, hostport = bare[:at], bare[at+1:]
        if ':' not in hostport: return None
        host, _, ps = hostport.rpartition(':')
        port = int(ps) if ps.isdigit() else -1
        c = userinfo.find(':')
        if c <= 0: return None
        raw_m, raw_p = userinfo[:c], userinfo[c+1:]
        if not raw_p:
            try:
                dec = base64.b64decode(raw_m + "="*(-len(raw_m)%4)).decode()
            except Exception: return None
            c2 = dec.find(':')
            if c2 <= 0: return None
            method, password = dec[:c2], dec[c2+1:]
        else:
            method, password = decode(raw_m), decode(raw_p)
    else:
        parts = bare.split(':')
        if len(parts) < 4: return None
        method, password = parts[0], ':'.join(parts[1:-2])
        host, ps = parts[-2], parts[-1]
        port = int(ps) if ps.isdigit() else -1
    if not method or not host or port <= 0: return None
    return {"outbounds":[{"type":"shadowsocks","tag":"proxy","server":host,"server_port":port,
                          "method":method,"password":password}]}

# --- hysteria2 ---
def parse_hy2(uri):
    p = up.urlparse(uri); q = query_params(p.query)
    if not p.hostname or not p.port: return None
    raw = p.username or ""
    pw = decode(raw)
    if p.password: pw += ":" + decode(p.password)
    ob = {"type":"hysteria2","tag":"proxy","server":p.hostname,"server_port":p.port,
          "password":pw,"tls":tls_block(q,p.hostname)}
    if qp(q,"obfs"): ob["obfs"] = {"type":qp(q,"obfs"),"password":qp(q,"obfs-password")}
    return {"outbounds":[ob]}

# --- tuic ---
def parse_tuic(uri):
    p = up.urlparse(uri); q = query_params(p.query)
    if not p.hostname or not p.port: return None
    # как в Java URI: весь userinfo до '@', вместе с паролем
    raw = uri.split('://',1)[1].split('@',1)[0] if '@' in uri.split('://',1)[1] else ''
    if not raw: return None
    if ':' in raw:
        u, _, pw = raw.partition(':')
        uuid_, password = decode(u), decode(pw)
    else:
        uuid_, password = decode(raw), ""
    ob = {"type":"tuic","tag":"proxy","server":p.hostname,"server_port":p.port,
          "uuid":uuid_,"password":password,"tls":tls_block(q,p.hostname)}
    if qp(q,"congestion_control"): ob["congestion_control"]=qp(q,"congestion_control")
    if qp(q,"udp_relay_mode"): ob["udp_relay_mode"]=qp(q,"udp_relay_mode")
    return {"outbounds":[ob]}

# --- awg ---
def parse_awg(text):
    iface, peer, section = {}, {}, ""
    for raw in text.lstrip("﻿").splitlines():
        line = raw.strip()
        if not line or line.startswith('#') or line.startswith(';'): continue
        if line.startswith('['):
            section = line.strip('[] ').lower(); continue
        if '=' in line:
            k, v = line.split('=',1)
            k, v = k.strip().lower(), v.split('#')[0].strip()
            if section == 'interface': iface[k]=v
            elif section == 'peer': peer.setdefault(k,v)
    endpoint = peer.get('endpoint','')
    if ':' not in endpoint or not iface.get('privatekey') or not peer.get('publickey'): return None
    host, _, ps = endpoint.rpartition(':')
    port = int(ps) if ps.isdigit() else -1
    addrs = [a.strip() for a in iface.get('address','10.8.0.2/32').split(',') if a.strip()]
    addrs = [a if '/' in a else (a+'/128' if ':' in a else a+'/32') for a in addrs]
    allowed = [a.strip() for a in peer.get('allowedips','0.0.0.0/0, ::/0').split(',') if a.strip()]
    return {"endpoints":[{"type":"wireguard","tag":"proxy","mtu":int(iface.get('mtu',1408)),
            "private_key":iface['privatekey'],"address":addrs,
            "peers":[{"address":host,"port":port,"public_key":peer['publickey'],"allowed_ips":allowed}]}]}

# ================= ПРОВЕРКИ (как в unit-тестах) =================
fails = []
def check(cond, msg):
    if not cond: fails.append(msg)

r = parse_vless("vless://uuid-1@example.com:443?security=reality&type=ws&path=%2Fws&host=cdn.example.com&sni=example.com&pbk=PBK&sid=SID&fp=chrome&flow=xtls-rprx-vision")
check(r is not None, "vless reality: None")
ob = r["outbounds"][0]
check(ob["type"]=="vless" and ob["tag"]=="proxy", "vless type/tag")
check(ob["server"]=="example.com" and ob["server_port"]==443, "vless server")
check(ob["flow"]=="xtls-rprx-vision", "vless flow")
check(ob["tls"]["reality"]["public_key"]=="PBK" and ob["tls"]["reality"]["short_id"]=="SID", "vless reality keys")
check(ob["transport"]["type"]=="ws" and ob["transport"]["path"]=="/ws", "vless transport ws path")
check(ob["transport"]["headers"]["Host"]=="cdn.example.com", "vless ws Host header")

r = parse_vless("vless://11111111-2222-3333-4444-555555555555@1.2.3.4:443")
ob = r["outbounds"][0]
check(ob["uuid"]=="11111111-2222-3333-4444-555555555555" and ob["server"]=="1.2.3.4", "vless simple")
check("tls" not in ob, "vless simple: лишний tls")

r = parse_trojan("trojan://p%40ss@t.example.com:8443?type=grpc&serviceName=svc&sni=t.example.com&alpn=h2%2Chttp%2F1.1")
ob = r["outbounds"][0]
check(ob["password"]=="p@ss", "trojan password decode")
check(ob["tls"]["server_name"]=="t.example.com", "trojan sni")
check(ob["transport"]["service_name"]=="svc", "trojan grpc")

r = parse_trojan("trojan://pass@1.2.3.4:443?security=none")
check("tls" not in r["outbounds"][0], "trojan security=none")

r = parse_ss("ss://aes-256-gcm:secret@1.2.3.4:8388")
ob = r["outbounds"][0]
check(ob["method"]=="aes-256-gcm" and ob["password"]=="secret" and ob["server"]=="1.2.3.4" and ob["server_port"]==8388, "ss new format")

enc = base64.b64encode(b"chacha20-ietf-poly1305:pass@ss.example.com:9000").decode()
r = parse_ss("ss://" + enc)
ob = r["outbounds"][0]
check(ob["server"]=="ss.example.com" and ob["server_port"]==9000 and ob["method"]=="chacha20-ietf-poly1305" and ob["password"]=="pass", "ss old base64 format")

r = parse_ss("ss://aes-256-gcm:secret:1.2.3.4:8388")
ob = r["outbounds"][0]
check(ob["server"]=="1.2.3.4" and ob["port" if False else "server_port"]==8388 and ob["method"]=="aes-256-gcm" and ob["password"]=="secret", "ss без @")

check(parse_ss("ss://aes-256-gcm:pass@1.2.3.4:8388?plugin=obfs-local%3Bobfs%3Dhttp") is None, "ss plugin должен отклоняться")

r = parse_hy2("hysteria2://pw@h2.example.com:443?obfs=salamander&obfs-password=obfspw&sni=h2.example.com")
ob = r["outbounds"][0]
check(ob["password"]=="pw" and ob["obfs"]["type"]=="salamander" and ob["obfs"]["password"]=="obfspw", "hy2 obfs")
check(parse_hy2("hy2://pw@h2.example.com:443") is not None, "hy2 alias")

r = parse_tuic("tuic://uuid-77:pass77@tuic.example.com:443?congestion_control=bbr&udp_relay_mode=quic")
ob = r["outbounds"][0]
check(ob["uuid"]=="uuid-77" and ob["password"]=="pass77" and ob["congestion_control"]=="bbr" and ob["udp_relay_mode"]=="quic", "tuic")

r = parse_vmess("vmess://" + base64.b64encode(json.dumps({"add":"5.6.7.8","port":"8080","id":"uuid-9","aid":"0","scy":"auto","net":"ws","path":"/vm","host":"h.example.com","tls":"tls","sni":"s.example.com"}).encode()).decode())
ob = r["outbounds"][0]
check(ob["server"]=="5.6.7.8" and ob["server_port"]==8080 and ob["uuid"]=="uuid-9" and ob["alter_id"]==0 and ob["security"]=="auto", "vmess поля")
check(ob["transport"]["type"]=="ws", "vmess transport")

conf = """[Interface]
PrivateKey = PRIVKEY=
Address = 10.8.0.2/32, fd00::2/128
DNS = 1.1.1.1
MTU = 1280

[Peer]
PublicKey = PUBKEY=
Endpoint = warp.example.com:2408
AllowedIPs = 0.0.0.0/0, ::/0"""
r = parse_awg(conf)
check(r is not None, "awg: None")
ep = r["endpoints"][0]
check(ep["type"]=="wireguard" and ep["private_key"]=="PRIVKEY=" and ep["mtu"]==1280, "awg interface")
check(ep["peers"][0]["address"]=="warp.example.com" and ep["peers"][0]["port"]==2408, "awg peer")
check(ep["address"]==["10.8.0.2/32","fd00::2/128"], "awg addresses: %r" % ep["address"])
check(parse_awg("[Interface]\nAddress = 10.8.0.2/32\n") is None, "awg без ключей должен быть None")

print("ПРОВАЛЕНО ПРОВЕРОК:", len(fails))
for f in fails: print("  -", f)
if not fails: print("ВСЕ ПРОВЕРКИ ПРОЙДЕНЫ")
