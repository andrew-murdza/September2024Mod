import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.amurdza.examplemod.worldgen.MountainRidges;
import net.amurdza.examplemod.worldgen.MountainLakeOffset;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.*;
import net.minecraft.world.level.levelgen.*;
import java.nio.file.*;
import java.util.*;

/** Uses Minecraft's codecs, seeded density functions and noise implementations. */
public final class SnowyCaveShapeAudit {
 static final Path ROOT=Path.of("src/main/resources/data/aoemod/worldgen/density_function");
 static HolderLookup.Provider lookup;
 static RegistryOps<JsonElement> ops;
 static NoiseGeneratorSettings vanilla;
 static class Parser {
  final Map<Double,DensityFunction> special=new HashMap<>();
  DensityFunction parse(String id)throws Exception{return parse(new JsonPrimitive(id));}
  DensityFunction parse(JsonElement json)throws Exception {
   JsonElement expanded=expand(json);
   return DensityFunction.DIRECT_CODEC.parse(ops,expanded).getOrThrow(false,System.err::println).mapAll(f->{
    if(f.minValue()==f.maxValue()&&special.containsKey(f.minValue()))return special.get(f.minValue());return f;
   });
  }
  JsonElement expand(JsonElement json)throws Exception {
   if(json.isJsonPrimitive()){
    if(json.getAsJsonPrimitive().isString()&&json.getAsString().startsWith("aoemod:snowy/")){
     String path=json.getAsString().substring("aoemod:".length());
     JsonElement read=JsonParser.parseString(Files.readString(ROOT.resolve(path+".json")));
     return expand(read);
    }
    return json.deepCopy();
   }
   if(json.isJsonArray()){JsonArray a=new JsonArray();for(var v:json.getAsJsonArray())a.add(expand(v));return a;}
   JsonObject o=json.getAsJsonObject();
   String type=o.has("type")?o.get("type").getAsString():"";
   if(type.equals("aoemod:mountain_ridges")){
    var noises=lookup.lookupOrThrow(Registries.NOISE);
    var ridge=noises.getOrThrow(ResourceKey.create(Registries.NOISE,new ResourceLocation(o.get("ridge").getAsString())));
    var shift=noises.getOrThrow(ResourceKey.create(Registries.NOISE,new ResourceLocation(o.get("shift").getAsString())));
    DensityFunction f=new MountainRidges(new DensityFunction.NoiseHolder(ridge),new DensityFunction.NoiseHolder(shift),o.get("stretch").getAsDouble());
    double marker=9000+special.size();special.put(marker,f);return new JsonPrimitive(marker);
   }
   if(type.equals("aoemod:mountain_lake_offset")){
    DensityFunction f=new MountainLakeOffset(parse(o.get("offset")),parse(o.get("ridges")),parse(o.get("continents")),parse(o.get("erosion")));
    double marker=9000+special.size();special.put(marker,f);return new JsonPrimitive(marker);
   }
   JsonObject result=new JsonObject();for(var entry:o.entrySet())result.add(entry.getKey(),expand(entry.getValue()));
   return result;
  }
 }
 static RandomState seeded(DensityFunction f,long seed){
  NoiseRouter r=vanilla.noiseRouter();
  NoiseRouter replaced=new NoiseRouter(r.barrierNoise(),r.fluidLevelFloodednessNoise(),r.fluidLevelSpreadNoise(),r.lavaNoise(),r.temperature(),r.vegetation(),r.continents(),r.erosion(),r.depth(),r.ridges(),r.initialDensityWithoutJaggedness(),f,r.veinToggle(),r.veinRidged(),r.veinGap());
  var cfg=new NoiseGeneratorSettings(vanilla.noiseSettings(),vanilla.defaultBlock(),vanilla.defaultFluid(),replaced,vanilla.surfaceRule(),vanilla.spawnTarget(),63,false,true,false,false);
  return RandomState.create(cfg,lookup.lookupOrThrow(Registries.NOISE),seed);
 }
 static DensityFunction raw(Parser parser,String name)throws Exception {
  JsonObject node=JsonParser.parseString(Files.readString(ROOT.resolve("snowy/"+name+".json"))).getAsJsonObject();
  if(node.get("type").getAsString().equals("minecraft:range_choice"))node=node.getAsJsonObject("when_out_of_range");
  while(!node.get("type").getAsString().equals("minecraft:interpolated"))
   node=node.getAsJsonObject(node.has("argument")?"argument":"argument2");
  return parser.parse(node.get("argument"));
 }
 static class Morphology {
  long air,closed,small,smallAir,closedAir,largest;ArrayList<Integer> sizes=new ArrayList<>();
  void add(Morphology m){air+=m.air;closed+=m.closed;small+=m.small;smallAir+=m.smallAir;closedAir+=m.closedAir;largest=Math.max(largest,m.largest);sizes.addAll(m.sizes);}
  public String toString(){sizes.sort(Integer::compareTo);return "air="+air+" enclosedComponents="+closed+" smallComponents="+small+" smallAir="+smallAir+" enclosedAir="+closedAir+" medianEnclosedVolume="+(sizes.isEmpty()?0:sizes.get(sizes.size()/2))+" largestEnclosed="+largest;}
 }
 static Morphology components(boolean[] air,int width){
  var stats=new Morphology();boolean[] visited=new boolean[air.length];int[] queue=new int[air.length];int plane=width*width;
  for(int start=0;start<air.length;start++){
   if(!air[start])continue;stats.air+=8;if(visited[start])continue;
   int head=0,tail=1;queue[0]=start;visited[start]=true;boolean boundary=false;
   while(head<tail){int cell=queue[head++],x=cell/plane,y=cell/width%width,z=cell%width;
    if(x==0||y==0||z==0||x==width-1||y==width-1||z==width-1)boundary=true;
    for(int d=0;d<6;d++){
     int next=switch(d){case 0->x>0?cell-plane:-1;case 1->x<width-1?cell+plane:-1;case 2->y>0?cell-width:-1;case 3->y<width-1?cell+width:-1;case 4->z>0?cell-1:-1;default->z<width-1?cell+1:-1;};
     if(next>=0&&air[next]&&!visited[next]){visited[next]=true;queue[tail++]=next;}
    }
   }
   if(!boundary){int volume=tail*8;stats.closed++;stats.closedAir+=volume;stats.sizes.add(volume);stats.largest=Math.max(stats.largest,volume);if(volume<=512){stats.small++;stats.smallAir+=volume;}}
  }
  return stats;
 }
 static double[] grid(DensityFunction function,int minX,int minY,int minZ){
  double[] values=new double[33*17*33];
  for(int x=0;x<33;x++)for(int y=0;y<17;y++)for(int z=0;z<33;z++)values[(x*17+y)*33+z]=function.compute(new DensityFunction.SinglePointContext(minX+x*4,minY+y*8,minZ+z*4));
  return values;
 }
 static double sample(double[] grid,int x,int y,int z){
  int gx=x/4,gy=y/8,gz=z/4;double tx=x%4/4.0,ty=y%8/8.0,tz=z%4/4.0,result=0;
  for(int dx=0;dx<2;dx++)for(int dy=0;dy<2;dy++)for(int dz=0;dz<2;dz++)result+=grid[((gx+dx)*17+gy+dy)*33+gz+dz]*(dx==0?1-tx:tx)*(dy==0?1-ty:ty)*(dz==0?1-tz:tz);
  return result;
 }
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.tryDetectVersion();try{net.minecraft.server.Bootstrap.bootStrap();}catch(LinkageError expected){}
  lookup=VanillaRegistries.createLookup();ops=RegistryOps.create(JsonOps.INSTANCE,lookup);
  vanilla=lookup.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
  var parser=new Parser();
  var previous=parser.parse("aoemod:snowy/previous_final_density");var current=parser.parse("aoemod:snowy/final_density");
  var base=parser.parse("aoemod:snowy/vanilla_final_density");var terrain=parser.parse("aoemod:snowy/terrain_density");var ridge=parser.parse("aoemod:snowy/ridges");
  var oldRaw=raw(parser,"previous_final_density");var newRaw=raw(parser,"final_density");var terrainRaw=raw(parser,"terrain_density");
  long oldTotal=0,newTotal=0,vanillaTotal=0;var oldMorph=new Morphology();var newMorph=new Morphology();
  for(long seed:new long[]{12345,98765,-8152788754161584091L}){
   var old=seeded(previous,seed).router().finalDensity();var next=seeded(current,seed).router().finalDensity();var solid=seeded(terrain,seed).router().finalDensity();var habitat=seeded(ridge,seed).router().finalDensity();var nativeDensity=seeded(base,seed).router().finalDensity();
   long before=0,after=0,vanillaAir=0;
   for(int x=-1536;x<=1536;x+=96)for(int z=-1536;z<=1536;z+=96){
    if(Math.abs(habitat.compute(new DensityFunction.SinglePointContext(x,0,z)))<.1)continue;
    for(int y=-60;y<=100;y+=8){var point=new DensityFunction.SinglePointContext(x,y,z);if(solid.compute(point)<=0)continue;if(old.compute(point)<=0)before++;if(next.compute(point)<=0)after++;if(nativeDensity.compute(point)<=0)vanillaAir++;}
   }
   oldTotal+=before;newTotal+=after;vanillaTotal+=vanillaAir;
   System.out.printf("VOLUME seed=%d previous=%d current=%d vanilla=%d ratio=%.6f%n",seed,before,after,vanillaAir,after/(double)before);
   var beforeRaw=seeded(oldRaw,seed).router().finalDensity();var afterRaw=seeded(newRaw,seed).router().finalDensity();var uncarved=seeded(terrainRaw,seed).router().finalDensity();
   for(int[] box:new int[][]{{-512,-512},{512,512},{1024,-1024}}){
    double[] oldGrid=grid(beforeRaw,box[0],-56,box[1]),newGrid=grid(afterRaw,box[0],-56,box[1]),terrainGrid=grid(uncarved,box[0],-56,box[1]);
    boolean[] oldAir=new boolean[64*64*64],newAir=new boolean[64*64*64];
    for(int x=0;x<64;x++)for(int z=0;z<64;z++){
     int wx=box[0]+x*2,wz=box[1]+z*2;
     if(Math.abs(habitat.compute(new DensityFunction.SinglePointContext(wx,0,wz)))<.1||Math.abs((long)((float)habitat.compute(new DensityFunction.SinglePointContext(Math.floorDiv(wx,4)*4,0,Math.floorDiv(wz,4)*4))*10000))<1000)continue;
     for(int y=0;y<64;y++){int index=(x*64+y)*64+z;if(sample(terrainGrid,x*2,y*2,z*2)<=0)continue;oldAir[index]=sample(oldGrid,x*2,y*2,z*2)<=0;newAir[index]=sample(newGrid,x*2,y*2,z*2)<=0;}
    }
    var oldStats=components(oldAir,64);var newStats=components(newAir,64);oldMorph.add(oldStats);newMorph.add(newStats);
    System.out.println("MORPH seed="+seed+" box="+Arrays.toString(box)+" previous{"+oldStats+"} current{"+newStats+"}");
   }
  }
  System.out.printf("TOTAL previous=%d current=%d vanilla=%d currentPreviousRatio=%.6f currentVanillaRatio=%.6f%n",oldTotal,newTotal,vanillaTotal,newTotal/(double)oldTotal,newTotal/(double)vanillaTotal);
  System.out.println("MORPH_TOTAL previous{"+oldMorph+"} current{"+newMorph+"}");
  if(newTotal<oldTotal*.99||newTotal>oldTotal*1.05)throw new AssertionError("Expanded noise-cave volume drifted");
  if(newMorph.small>=oldMorph.small||newMorph.smallAir>=oldMorph.smallAir)throw new AssertionError("Small enclosed cavities did not decrease");
 }
}
