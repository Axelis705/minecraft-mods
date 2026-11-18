#!/usr/bin/env python3
"""Genera el modelo (Java), la textura y el brillo del Archimago a partir de una lista de cajas. Modelo provisional hecho por código:
se puede sustituir por uno hecho en Blockbench conservando los nombres de las partes."""
import random, os, sys
from PIL import Image, ImageDraw
random.seed(7)
OUT_JAVA = sys.argv[1]; OUT_TEX = sys.argv[2]
TW = TH = 128
INDIGO=(43,35,96); INDIGO2=(30,24,70); PURPLE=(96,66,176); GOLD=(222,180,74); SKIN=(205,196,220); BEARD=(238,238,246)
WOOD=(64,44,34); CYAN=(120,240,255); MAGENTA=(220,110,255); BOOK=(110,60,40); PAGE=(240,230,200)
# parte -> (padre, pivote), cajas: (parte, x,y,z,w,h,d, color, brillo, etiqueta)
parts = [
 ("body", None, (0,0,0)),
 ("head", "body", (0,0,0)),
 ("hat", "head", (0,-8,0)),
 ("hat_tip", "hat", (0,-12,0)),
 ("right_arm", "body", (-7,2,0)),
 ("left_arm", "body", (7,2,0)),
 ("staff", "right_arm", (-1,10,-2)),
 ("skirt", "body", (0,12,0)),
 ("book", None, (11,6,-5)),
 ("crystal1", None, (0,0,0)),
 ("crystal2", None, (0,0,0)),
 ("crystal3", None, (0,0,0)),
]
boxes = [
 ("body",-5,0,-3,10,12,6,INDIGO,None,"torso"),
 ("body",-6,-1,-4,12,4,8,PURPLE,None,"mantle"),
 ("body",-5,10,-3.5,10,2,7,GOLD,None,"belt"),
 ("head",-4,-8,-4,8,8,8,SKIN,None,"face"),
 ("head",-3,-2,-5,6,7,2,BEARD,None,"beard"),
 ("head",-2,5,-4.6,4,4,1,BEARD,None,"beard2"),
 ("hat",-7,-1,-7,14,1,14,INDIGO2,None,"brim"),
 ("hat",-4,-5,-4,8,4,8,INDIGO,None,"cone1"),
 ("hat",-3,-9,-3,6,4,6,INDIGO,None,"cone2"),
 ("hat",-4.5,-2,-4.5,9,1,9,GOLD,None,"band"),
 ("hat_tip",-2,-1,-2,4,4,4,INDIGO,None,"cone3"),
 ("hat_tip",-1,-4,0,2,3,2,INDIGO2,None,"cone4"),
 ("hat_tip",-1,-6,1,2,2,2,CYAN,CYAN,"hatgem"),
 ("right_arm",-3,-2,-2.5,4,10,5,INDIGO,None,"sleeve"),
 ("right_arm",-3.5,6,-3,5,4,6,PURPLE,None,"cuff"),
 ("right_arm",-2,10,-1,2,2,2,SKIN,None,"hand"),
 ("left_arm",-1,-2,-2.5,4,10,5,INDIGO,None,"sleeve"),
 ("left_arm",-1.5,6,-3,5,4,6,PURPLE,None,"cuff"),
 ("left_arm",0,10,-1,2,2,2,SKIN,None,"hand"),
 ("staff",-0.5,-22,-0.5,1,34,1,WOOD,None,"pole"),
 ("staff",-1.5,-24,-1.5,3,2,3,GOLD,None,"cap"),
 ("staff",-2.5,-28,-0.5,1,5,1,GOLD,None,"prong"),
 ("staff",1.5,-28,-0.5,1,5,1,GOLD,None,"prong"),
 ("staff",-1.5,-30,-1.5,3,5,3,CYAN,CYAN,"gem"),
 ("skirt",-6,0,-4,12,6,8,INDIGO,None,"skirt1"),
 ("skirt",-7,6,-5,14,5,10,INDIGO2,None,"skirt2"),
 ("skirt",-7.5,10,-5.5,15,1,11,GOLD,GOLD,"hem"),
 ("book",-3,-4,-1,6,8,2,BOOK,None,"book"),
 ("book",-2.5,-3.5,-1.4,5,7,1,PAGE,(90,200,255),"pages"),
 ("crystal1",-1.5,-3,-1.5,3,6,3,CYAN,CYAN,"cr"),
 ("crystal1",-1,-4.5,-1,2,9,2,(190,250,255),(190,250,255),"cr2"),
 ("crystal2",-1.5,-3,-1.5,3,6,3,MAGENTA,MAGENTA,"cr"),
 ("crystal2",-1,-4.5,-1,2,9,2,(240,190,255),(240,190,255),"cr2"),
 ("crystal3",-1.5,-3,-1.5,3,6,3,GOLD,GOLD,"cr"),
 ("crystal3",-1,-4.5,-1,2,9,2,(255,240,180),(255,240,180),"cr2"),
]
# --- empaquetado de UV por estantes
items = sorted(range(len(boxes)), key=lambda i: -(boxes[i][5]+boxes[i][6]))
x=y=0; shelf=0; uv={}
for i in items:
    _,_,_,_,w,h,d,_,_,_=boxes[i]
    bw=2*(w+d); bh=d+h
    if x+bw>TW: x=0; y+=shelf; shelf=0
    assert y+bh<=TH, "no cabe"
    uv[i]=(x,y); x+=bw; shelf=max(shelf,bh)
tex=Image.new("RGBA",(TW,TH),(0,0,0,0)); glow=Image.new("RGBA",(TW,TH),(0,0,0,255))
def shade(c,k): return tuple(max(0,min(255,int(v*k))) for v in c)
def fill(img,x0,y0,w,h,c,noise=0.07,k=1.0):
    px=img.load()
    for yy in range(y0,y0+h):
        for xx in range(x0,x0+w):
            n=1+random.uniform(-noise,noise)
            px[xx,yy]=shade(c,k*n)+(255,)
for i,b in enumerate(boxes):
    part,bx,by,bz,w,h,d,col,gl,tag=b
    u,v=uv[i]
    faces={"top":(u+d,v,w,d,1.12),"bottom":(u+d+w,v,w,d,0.62),"right":(u,v+d,d,h,0.84),"front":(u+d,v+d,w,h,1.0),
           "left":(u+d+w,v+d,d,h,0.84),"back":(u+d+w+d,v+d,w,h,0.74)}
    for name,(fx,fy,fw,fh,k) in faces.items():
        fill(tex,fx,fy,fw,fh,col,k=k)
        if gl is not None:
            fill(glow,fx,fy,fw,fh,gl,noise=0.03,k=0.95 if name!="bottom" else 0.6)
    fx,fy,fw,fh,_=faces["front"]
    dr=ImageDraw.Draw(tex); dg=ImageDraw.Draw(glow)
    if tag=="face":
        # cejas, ojos que brillan, nariz
        for ex in (1,5):
            dr.rectangle([fx+ex,fy+3,fx+ex+1,fy+3],fill=CYAN+(255,)); dg.rectangle([fx+ex,fy+3,fx+ex+1,fy+3],fill=CYAN+(255,))
            dr.rectangle([fx+ex,fy+2,fx+ex+1,fy+2],fill=shade(BEARD,0.9)+(255,))
        dr.rectangle([fx+3,fy+4,fx+4,fy+5],fill=shade(SKIN,0.85)+(255,))
    if tag=="torso":
        # franja dorada central y runa en el pecho
        dr.rectangle([fx+4,fy,fx+5,fy+fh-1],fill=GOLD+(255,))
        dr.rectangle([fx+3,fy+3,fx+6,fy+5],fill=CYAN+(255,)); dg.rectangle([fx+3,fy+3,fx+6,fy+5],fill=shade(CYAN,0.9)+(255,))
    if tag=="mantle":
        for f in ("front","back","left","right"):
            x0,y0,w0,h0,_=faces[f]; dr.rectangle([x0,y0+h0-1,x0+w0-1,y0+h0-1],fill=GOLD+(255,))
    if tag in ("skirt1","skirt2","cone1","cone2"):
        # estrellitas doradas
        for f in ("front","back","left","right"):
            x0,y0,w0,h0,_=faces[f]
            for _ in range(max(1,w0*h0//22)):
                sx=x0+random.randrange(w0); sy=y0+random.randrange(h0)
                tex.putpixel((sx,sy),GOLD+(255,)); glow.putpixel((sx,sy),shade(GOLD,0.55)+(255,))
    if tag=="book":
        dr.rectangle([fx+1,fy+1,fx+fw-2,fy+fh-2],outline=GOLD+(255,))
    if tag=="beard":
        for xx in range(fx,fx+fw,2): dr.line([xx,fy+1,xx,fy+fh-1],fill=shade(BEARD,0.88)+(255,))
os.makedirs(os.path.dirname(OUT_TEX),exist_ok=True)
tex.save(OUT_TEX); glow.save(OUT_TEX.replace(".png","_glow.png"))
# --- Java
def f(v): return ("%sF" % (int(v) if float(v).is_integer() else v)).replace("FF","F") if True else ""
def fl(v):
    v=float(v); return ("%d.0F"%int(v)) if v.is_integer() else ("%sF"%v)
lines=[]
for name,parent,piv in parts:
    cubes="CubeListBuilder.create()"
    for i,b in enumerate(boxes):
        if b[0]!=name: continue
        cubes+="\n                .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)"%(uv[i][0],uv[i][1],fl(b[1]),fl(b[2]),fl(b[3]),fl(b[4]),fl(b[5]),fl(b[6]))
    par="root" if parent is None else parent
    lines.append('        PartDefinition %s = %s.addOrReplaceChild("%s", %s,\n                PartPose.offset(%s, %s, %s));'%(name,par,name,cubes,fl(piv[0]),fl(piv[1]),fl(piv[2])))
open(OUT_JAVA,"w",encoding="utf-8").write("\n".join(lines)+"\n")
# --- vista previa frontal sencilla (cajas por profundidad)
piv={n:(pp,p) for n,pp,p in parts}
def absp(n):
    x=y=z=0
    while n is not None:
        pp,p=piv[n]; x+=p[0]; y+=p[1]; z+=p[2]; n=pp
    return x,y,z
cr={"crystal1":(-13,-4,0),"crystal2":(13,-10,2),"crystal3":(0,-30,6)}
S=8; im=Image.new("RGB",(60*S,64*S),(24,22,34)); d=ImageDraw.Draw(im)
order=sorted(range(len(boxes)),key=lambda i:-(boxes[i][3]+absp(boxes[i][0])[2]))
for i in order:
    part,bx,by,bz,w,h,dd,col,gl,tag=boxes[i]
    ax,ay,az=absp(part)
    if part in cr: ax,ay,az=cr[part]
    u,v=uv[i]
    face=tex.crop((u+dd,v+dd,u+dd+w,v+dd+h)).resize((int(w*S),int(h*S)),Image.NEAREST)
    im.paste(face,(int((30+ax+bx)*S),int((34+ay+by)*S)))
im.save(OUT_TEX.replace(".png","_preview.png"))
print("ok",len(boxes),"cajas")
