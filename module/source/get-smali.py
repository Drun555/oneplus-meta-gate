import urllib.request,pathlib,concurrent.futures,zipfile
p=pathlib.Path('work/tools');p.mkdir(exist_ok=True)
a=[('org.smali',x,'2.5.2') for x in ['baksmali','smali','dexlib2','util']]+[('com.google.guava','guava','27.1-android'),('com.beust','jcommander','1.64'),('org.antlr','antlr','3.5.2'),('org.antlr','antlr-runtime','3.5.2'),('org.antlr','stringtemplate','3.2.1')]
def get(t):
 g,n,v=t;urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/'+g.replace('.','/')+'/'+n+'/'+v+'/'+n+'-'+v+'.jar',p/(n+'.jar'))
with concurrent.futures.ThreadPoolExecutor() as e:list(e.map(get,a))
with zipfile.ZipFile('work/services.jar') as z:
 for n in z.namelist():
  if n.endswith('.dex'):pathlib.Path('work/'+n).write_bytes(z.read(n))
