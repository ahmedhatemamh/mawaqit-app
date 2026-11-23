import json,glob,math,datetime as dt,re,sys
S=sys.argv[1]
def times(y,m,d,lat,lon,tz,fa=19.5,ia=17.5):
    jd=dt.date(y,m,d).toordinal()-dt.date(2000,1,1).toordinal()-lon/360
    g=math.radians((357.529+0.98560028*jd)%360); q=(280.459+0.98564736*jd)%360
    L=math.radians((q+1.915*math.sin(g)+0.020*math.sin(2*g))%360); e=math.radians(23.439-0.00000036*jd)
    RA=math.degrees(math.atan2(math.cos(e)*math.sin(L),math.cos(L)))/15%24
    dec=math.asin(math.sin(e)*math.sin(L)); eqt=q/15-RA
    eqt=(eqt+12)%24-12
    noon=12+tz-lon/15-eqt; la=math.radians(lat)
    T=lambda a:math.degrees(math.acos((-math.sin(math.radians(a))-math.sin(la)*math.sin(dec))/(math.cos(la)*math.cos(dec))))/15
    alt=math.degrees(math.atan(1/(1+math.tan(abs(la-dec)))))
    TA=math.degrees(math.acos((math.sin(math.radians(alt))-math.sin(la)*math.sin(dec))/(math.cos(la)*math.cos(dec))))/15
    sr=T(0.833)
    return [noon-T(fa),noon-sr,noon,noon+TA,noon+sr,noon+T(ia)]
def mins(s):
    m=re.match(r"(\d+):(\d+) (ص|م)",s); h=int(m.group(1))%12+(12 if m.group(3)=="م" else 0); return h*60+int(m.group(2))
rows=[]
for f in sorted(glob.glob(S+"/esa_year/2026-*.json")):
    iso=f.split("/")[-1][:-5]; r=json.load(open(f))
    c=[x for x in r if x[0].replace("ـ","")=="القاهرة"][0]
    rows.append((iso,[mins(v) for v in c[3:9]],c[2]))
print("date, ESA noon(dhuhr) Cairo, Hijri")
for iso,v,h in rows[::6]: print(iso,"%d:%02d"%(v[2]//60,v[2]%60),h)
dstflip=[(iso,v[2]//60) for iso,v,h in rows if iso in("2026-04-23","2026-04-24","2026-04-25","2026-10-28","2026-10-29","2026-10-30")]
print("around DST dates dhuhr hour:",dstflip)
# compare formula, choose tz by ESA dhuhr hour (DST if >=12:20 roughly)
worst=[0]*6; cnt=0; ex=[0]*6
for iso,v,h in rows:
    y,m,d=map(int,iso.split("-")); dst=v[2]>=12*60+20
    c=times(y,m,d,30.0444,31.2357,3 if dst else 2)
    for k in range(6):
        diff=round(c[k]*60)-v[k]
        worst[k]=max(worst[k],abs(diff)); ex[k]+= (diff!=0)
    cnt+=1
print("days",cnt,"max abs diff minutes per prayer [fajr,sunrise,dhuhr,asr,maghrib,isha]:",worst)
print("days with nonzero diff per prayer:",ex)
