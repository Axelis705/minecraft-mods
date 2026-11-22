import numpy as np, math, re, sys
def Rx(a):
    a=math.radians(a);c,s=math.cos(a),math.sin(a);return np.array([[1,0,0,0],[0,c,-s,0],[0,s,c,0],[0,0,0,1]])
def Ry(a):
    a=math.radians(a);c,s=math.cos(a),math.sin(a);return np.array([[c,0,s,0],[0,1,0,0],[-s,0,c,0],[0,0,0,1]])
def Rz(a):
    a=math.radians(a);c,s=math.cos(a),math.sin(a);return np.array([[c,-s,0,0],[s,c,0,0],[0,0,1,0],[0,0,0,1]])
def T(x,y,z):
    m=np.eye(4);m[:3,3]=[x,y,z];return m
def S(k):
    m=np.eye(4);m[0,0]=m[1,1]=m[2,2]=k;return m
src=open('/home/claude/axelis/src/main/java/com/mundosdeaxelis/client/ArmaAnim.java').read()
def keys(name):
    blk=re.search(r'float\[\]\[\] '+name+r' = \{(.*?)\};',src,re.S).group(1)
    rows=re.findall(r'\{([^{}]*)\}',blk)
    return [[float(x.strip().rstrip('F')) for x in r.split(',')] for r in rows]
def pose(k,t):
    s=0
    while s<len(k)-2 and t>k[s+1][0]: s+=1
    a,b=k[s],k[s+1]
    u=min(1,max(0,(t-a[0])/max(1e-4,b[0]-a[0]))); u=u*u*(3-2*u)
    return [a[j+1]+(b[j+1]-a[j+1])*u for j in range(6)]
MANO={'espada':(1.0,[1.13,3.2,1.13]),'dagas':(1.0,[1.13,3.2,1.13]),'hacha':(1.15,[1.13,4.2,1.13]),'martillo':(1.2,[1.13,4.6,1.13]),'lanza':(1.45,[1.13,2.4,1.13])}
# punto de agarre y punta (pixeles del dibujo de 32)
PTS={'espada':[(5,26),(29,2)],'hacha':[(4,27),(26,6)],'martillo':(lambda:[(4,27),(26,5)])(),'lanza':[(12,19),(30,1)],'dagas':[(6,25),(19,12)]}
def proj(tipo,t,fov=70,asp=16/9):
    k=pose(keys(tipo.upper()),t)
    s,tr=MANO[tipo]
    disp=T(tr[0]/16,tr[1]/16,tr[2]/16)@Rx(0)@Ry(-90)@Rz(25)@S(0.68*s)@T(-0.5,-0.5,-0.5)
    i=1
    M=T(i*0.56,-0.52,-0.72)@T(i*k[0],k[1],k[2])@Ry(i*k[3])@Rz(i*k[4])@Rx(k[5])@Ry(-i*k[3])@disp
    out=[]
    for px,py in PTS[tipo]:
        v=M@np.array([px/32,1-py/32,0.5,1])
        x,y,z=v[:3]
        f=1/math.tan(math.radians(fov/2))
        sx=(x/-z)*f/asp; sy=(y/-z)*f
        out.append((round((sx+1)/2*1280),round((1-sy)/2*720),round(z,2)))
    return out
for tipo in ['espada','hacha','martillo','lanza','dagas']:
    k=keys(tipo.upper())
    print(tipo, ' '.join('t%.2f:%s'%(t, proj(tipo,t)) for t in [0]+[r[0] for r in k[1:-1]]))

def screen(tipo,k):
    s,tr=MANO[tipo]
    disp=T(tr[0]/16,tr[1]/16,tr[2]/16)@Ry(-90)@Rz(25)@S(0.68*s)@T(-0.5,-0.5,-0.5)
    M=T(0.56,-0.52,-0.72)@T(k[0],k[1],k[2])@Ry(k[3])@Rz(k[4])@Rx(k[5])@Ry(-k[3])@disp
    out=[]
    for px,py in PTS[tipo]:
        v=M@np.array([px/32,1-py/32,0.5,1]); x,y,z=v[:3]
        f=1/math.tan(math.radians(35))
        if z>-0.05: return None
        out.append(((x/-z)*f/(16/9)*640+640,(1-(y/-z)*f)*360,z))
    return out
def search(tipo,grip,tip,phi_fixed=None,iters=60000,seed=1,zmin=None):
    rng=np.random.default_rng(seed); best=None
    for _ in range(iters):
        k=[rng.uniform(-0.5,0.3),rng.uniform(-0.35,0.3),rng.uniform(-0.7,0.2),
           phi_fixed if phi_fixed is not None else rng.uniform(-60,60),rng.uniform(-50,50),rng.uniform(-120,40)]
        sc=screen(tipo,k)
        if sc is None: continue
        e=math.dist(sc[0][:2],grip)+math.dist(sc[1][:2],tip)
        if zmin is not None and sc[1][2]>zmin: e+=1000*(sc[1][2]-zmin)
        e+=200*sum(abs(v) for v in k[:3])  # sin moverse de más
        if best is None or e<best[0]: best=(e,k,sc)
    return best
if len(sys.argv)>1 and sys.argv[1]=='buscar':
    for name,(tipo,grip,tip,phi,zmin) in {
        'lanza_embiste':('lanza',(960,700),(660,380),None,-2.0),
        'lanza_atras':('lanza',(1150,760),(900,330),None,-1.2),
        'dagas_punal':('dagas',(900,640),(700,420),None,-1.0),
        'hacha_arriba':('hacha',(1120,560),(1150,40),None,None),
        'martillo_arriba':('martillo',(1100,520),(1000,20),None,None),
    }.items():
        b=search(tipo,grip,tip,phi,zmin=zmin)
        print(name, 'err',round(b[0]), 'k',[round(v,2) for v in b[1]], 'pantalla',[(round(a),round(c),round(z,2)) for a,c,z in b[2]])
if len(sys.argv)>1 and sys.argv[1]=='todo':
    for tipo in ['espada','hacha','martillo','lanza','dagas']:
        k=keys(tipo.upper()); bad=[]
        for t in [x/40 for x in range(41)]:
            sc=screen(tipo,pose(k,t))
            if sc is None or any(z>-0.2 for _,_,z in sc) or not any(-100<a<1380 and -100<c<820 for a,c,_ in sc):
                bad.append(round(t,3))
        print(tipo,'fuera de pantalla en',bad)

def cara(tipo,k):
    s,tr=MANO[tipo]
    disp=T(tr[0]/16,tr[1]/16,tr[2]/16)@Ry(-90)@Rz(25)@S(0.68*s)@T(-0.5,-0.5,-0.5)
    M=T(0.56,-0.52,-0.72)@T(k[0],k[1],k[2])@Ry(k[3])@Rz(k[4])@Rx(k[5])@Ry(-k[3])@disp
    c=(M@np.array([0.5,0.5,0.5,1]))[:3]
    n=(M@np.array([0,0,1,0]))[:3]; n/=np.linalg.norm(n)
    return abs(float(np.dot(n,c/np.linalg.norm(c))))
if len(sys.argv)>1 and sys.argv[1]=='cara':
    for tipo in ['espada','hacha','martillo','lanza','dagas']:
        k=keys(tipo.upper())
        print(tipo,[(r[0],round(cara(tipo,pose(k,r[0])),2)) for r in k])

def search2(tipo,grip,tip,fmin=0.62,zr=(-1.0,-0.4),iters=120000,seed=3):
    rng=np.random.default_rng(seed); best=None
    for _ in range(iters):
        k=[rng.uniform(-0.5,0.3),rng.uniform(-0.4,0.35),rng.uniform(-0.5,0.25),
           rng.uniform(-70,70),rng.uniform(-60,60),rng.uniform(-110,50)]
        sc=screen(tipo,k)
        if sc is None: continue
        e=math.dist(sc[0][:2],grip)+math.dist(sc[1][:2],tip)
        f=cara(tipo,k)
        if f<fmin: e+=2000*(fmin-f)
        z=sc[1][2]
        if z<zr[0]: e+=1500*(zr[0]-z)
        if z>zr[1]: e+=1500*(z-zr[1])
        e+=120*sum(abs(v) for v in k[:3])
        if best is None or e<best[0]: best=(e,k,sc,f)
    return best
if len(sys.argv)>1 and sys.argv[1]=='buscar2':
    objetivos={
        'espada_arriba':('espada',(1120,640),(1250,140)),
        'espada_golpe':('espada',(900,700),(560,470)),
        'hacha_golpe':('hacha',(930,720),(700,520)),
        'martillo_arriba':('martillo',(1060,540),(1000,40)),
        'martillo_golpe':('martillo',(950,740),(720,560)),
        'dagas_prep':('dagas',(1120,760),(1080,520)),
        'dagas_golpe':('dagas',(900,650),(720,480)),
    }
    for name,(tipo,g,t) in objetivos.items():
        b=search2(tipo,g,t)
        print(name,'err',round(b[0]),'k',[round(v,2) for v in b[1]],'cara',round(b[3],2),'pantalla',[(round(a),round(c),round(z,2)) for a,c,z in b[2]],flush=True)
if len(sys.argv)>1 and sys.argv[1]=='todo2':
    for tipo in ['espada','hacha','martillo','lanza','dagas']:
        k=keys(tipo.upper()); rows=[]
        for t in [x/20 for x in range(21)]:
            kk=pose(k,t); sc=screen(tipo,kk); f=cara(tipo,kk)
            rows.append('%.2f:%s/%.2f'%(t,'X' if sc is None else '(%d,%d)'%(sc[1][0],sc[1][1]),f))
        print(tipo,' '.join(rows))
