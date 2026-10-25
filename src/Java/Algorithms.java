import java.util.*;

public final class Algorithms {
    private Algorithms() {}

    private static Map<String,Object> obj(Object... kv) {
        Map<String,Object> m = new LinkedHashMap<>();
        for (int i=0;i<kv.length;i+=2) m.put((String)kv[i], kv[i+1]);
        return m;
    }

    private static final class DSU {
        int[] parent, size;
        int components, largest;
        DSU(int n) {
            parent=new int[n]; size=new int[n];
            components=n; largest=n==0?0:1;
            for(int i=0;i<n;i++){parent[i]=i;size[i]=1;}
        }
        int find(int x) {
            int r=x;
            while(parent[r]!=r) r=parent[r];
            while(parent[x]!=x){int p=parent[x];parent[x]=r;x=p;}
            return r;
        }
        boolean union(int a,int b) {
            a=find(a); b=find(b);
            if(a==b)return false;
            if(size[a]<size[b]){int t=a;a=b;b=t;}
            parent[b]=a; size[a]+=size[b]; components--;
            largest=Math.max(largest,size[a]); return true;
        }
    }

    static Map<String,Object> dsu(Map<String,Object> input) {
        int n=Input.vertexCount(input); int[][] ops=Input.operations(input);
        DSU d=new DSU(n); List<List<Integer>> states=new ArrayList<>(ops.length);
        for(int[] op:ops){d.union(op[0],op[1]);states.add(Arrays.asList(d.components,d.largest));}
        return obj("states",states);
    }

    static Map<String,Object> kruskal(Map<String,Object> input) {
        int n=Input.vertexCount(input); int[][] edges=Input.edges(input);
        Integer[] ids=new Integer[edges.length]; for(int i=0;i<ids.length;i++)ids[i]=i;
        Arrays.sort(ids,(a,b)->{int c=Integer.compare(edges[a][2],edges[b][2]);return c!=0?c:Integer.compare(a,b);});
        DSU d=new DSU(n); long cost=0; List<Integer> chosen=new ArrayList<>();
        for(int id:ids){int[] e=edges[id];if(d.union(e[0],e[1])){cost+=(long)e[2];chosen.add(id);}}
        return obj("cost",cost,"edge_ids",chosen,"components",d.components);
    }

    private static final class AdjEdge {
        int to,id,w;
        AdjEdge(int to,int id,int w){this.to=to;this.id=id;this.w=w;}
    }
    private static final class Candidate {
        int from,to,id,w;
        Candidate(int from,int to,int id,int w){this.from=from;this.to=to;this.id=id;this.w=w;}
    }

    static Map<String,Object> prim(Map<String,Object> input) {
        int n=Input.vertexCount(input); int[][] edges=Input.edges(input);
        List<List<AdjEdge>> adj=new ArrayList<>(n);
        for(int i=0;i<n;i++)adj.add(new ArrayList<>());
        for(int id=0;id<edges.length;id++){
            int[] e=edges[id]; adj.get(e[0]).add(new AdjEdge(e[1],id,e[2]));
            if(e[0]!=e[1])adj.get(e[1]).add(new AdjEdge(e[0],id,e[2]));
        }
        PriorityQueue<Candidate> pq=new PriorityQueue<>((a,b)->{
            int c=Integer.compare(a.w,b.w); if(c!=0)return c;
            c=Integer.compare(a.id,b.id); if(c!=0)return c;
            return Integer.compare(a.to,b.to);
        });
        boolean[] seen=new boolean[n]; List<Integer> chosen=new ArrayList<>();
        long cost=0; int components=0;
        for(int start=0;start<n;start++){
            if(seen[start])continue;
            components++; seen[start]=true;
            for(AdjEdge e:adj.get(start))if(!seen[e.to])pq.add(new Candidate(start,e.to,e.id,e.w));
            while(!pq.isEmpty()){
                Candidate c=pq.poll();
                if(seen[c.to])continue;
                seen[c.to]=true; chosen.add(c.id); cost+=(long)c.w;
                for(AdjEdge e:adj.get(c.to))if(!seen[e.to])pq.add(new Candidate(c.to,e.to,e.id,e.w));
            }
        }
        return obj("cost",cost,"edge_ids",chosen,"components",components);
    }

    static Map<String,Object> scc(Map<String,Object> input) {
        int n=Input.vertexCount(input); int[][] edges=Input.edges(input);
        List<List<Integer>> g=new ArrayList<>(n), rev=new ArrayList<>(n);
        for(int i=0;i<n;i++){g.add(new ArrayList<>());rev.add(new ArrayList<>());}
        for(int[] e:edges){g.get(e[0]).add(e[1]);rev.get(e[1]).add(e[0]);}
        boolean[] seen=new boolean[n]; int[] order=new int[n]; int count=0;
        int[] stackV=new int[n], stackI=new int[n];
        for(int s=0;s<n;s++)if(!seen[s]){
            int top=0;stackV[0]=s;stackI[0]=0;seen[s]=true;
            while(top>=0){
                int v=stackV[top];
                if(stackI[top]<g.get(v).size()){
                    int to=g.get(v).get(stackI[top]++);
                    if(!seen[to]){seen[to]=true;top++;stackV[top]=to;stackI[top]=0;}
                } else {order[count++]=v;top--;}
            }
        }
        int[] comp=new int[n]; Arrays.fill(comp,-1); int label=0;
        int[] stack=new int[n];
        for(int k=count-1;k>=0;k--){
            int s=order[k];if(comp[s]!=-1)continue;
            int top=0;stack[0]=s;comp[s]=label;
            while(top>=0){
                int v=stack[top--];
                for(int to:rev.get(v))if(comp[to]==-1){comp[to]=label;stack[++top]=to;}
            }
            label++;
        }
        Set<Long> pairs=new HashSet<>();
        for(int[] e:edges)if(comp[e[0]]!=comp[e[1]])
            pairs.add((((long)comp[e[0]])<<32) | (comp[e[1]]&0xffffffffL));
        List<List<Integer>> condensation=new ArrayList<>(pairs.size());
        for(long p:pairs)condensation.add(Arrays.asList((int)(p>>32),(int)p));
        condensation.sort((a,b)->{int c=Integer.compare(a.get(0),b.get(0));return c!=0?c:Integer.compare(a.get(1),b.get(1));});
        List<Integer> labels=new ArrayList<>(n);for(int x:comp)labels.add(x);
        return obj("component",labels,"condensation",condensation);
    }

    private static final class Frame {int v,next; Frame(int v){this.v=v;}}
    private static final class TopoResult {boolean acyclic; int[] order; TopoResult(boolean a,int[] o){acyclic=a;order=o;}}
    private static TopoResult topological(int n, List<List<AdjEdge>> adj) {
        byte[] state=new byte[n]; int[] order=new int[n]; int count=0;
        Frame[] stack=new Frame[n];
        for(int s=0;s<n;s++){
            if(state[s]!=0)continue;
            int top=0;stack[0]=new Frame(s);state[s]=1;
            while(top>=0){
                Frame f=stack[top]; List<AdjEdge> list=adj.get(f.v);
                if(f.next<list.size()){
                    int to=list.get(f.next++).to;
                    if(state[to]==1)return new TopoResult(false,new int[0]);
                    if(state[to]==0){state[to]=1;stack[++top]=new Frame(to);}
                } else {state[f.v]=2;order[count++]=f.v;top--;}
            }
        }
        for(int i=0;i<count/2;i++){int t=order[i];order[i]=order[count-1-i];order[count-1-i]=t;}
        return new TopoResult(true,order);
    }

    private static List<List<AdjEdge>> directedAdj(int n,int[][] edges) {
        List<List<AdjEdge>> adj=new ArrayList<>(n);
        for(int i=0;i<n;i++)adj.add(new ArrayList<>());
        for(int id=0;id<edges.length;id++){int[] e=edges[id];adj.get(e[0]).add(new AdjEdge(e[1],id,e.length>2?e[2]:0));}
        return adj;
    }
    static Map<String,Object> topo(Map<String,Object> input) {
        int n=Input.vertexCount(input); int[][] edges=Input.edges(input);
        TopoResult r=topological(n,directedAdj(n,edges));
        List<Integer> order=new ArrayList<>();for(int v:r.order)order.add(v);
        return obj("acyclic",r.acyclic,"order",order);
    }

    static List<Integer> reconstructRoute(int source,int target,int[] parentEdge,int[][] edges) {
        if(source<0||target<0||source>=parentEdge.length||target>=parentEdge.length)throw new IllegalArgumentException("vertex out of range");
        if(target==source)return new ArrayList<>(List.of(source));
        List<Integer> reversed=new ArrayList<>(); int cur=target;
        for(int steps=0;steps<parentEdge.length;steps++){
            if(cur==source){reversed.add(source);Collections.reverse(reversed);return reversed;}
            int id=parentEdge[cur];
            if(id<0||id>=edges.length)return new ArrayList<>();
            int[] e=edges[id]; if(e[1]!=cur)return new ArrayList<>();
            reversed.add(cur);cur=e[0];
        }
        return new ArrayList<>();
    }

    static Map<String,Object> dag(Map<String,Object> input) {
        int n=Input.vertexCount(input), source=Input.source(input);int[][] edges=Input.edges(input);
        List<List<AdjEdge>> adj=directedAdj(n,edges);
        TopoResult r=topological(n,adj);
        if(!r.acyclic)return obj("acyclic",false,"distance",new ArrayList<>(),"parent_edge",new ArrayList<>());
        long[] dist=new long[n];boolean[] reachable=new boolean[n];int[] parent=new int[n];Arrays.fill(parent,-1);reachable[source]=true;
        for(int v:r.order)if(reachable[v]){
            for(AdjEdge e:adj.get(v)){
                long nd=dist[v]+(long)e.w;
                if(!reachable[e.to]||nd<dist[e.to]){reachable[e.to]=true;dist[e.to]=nd;parent[e.to]=e.id;}
            }
        }
        List<Object> distances=new ArrayList<>(n);List<Integer> parents=new ArrayList<>(n);
        for(int i=0;i<n;i++){distances.add(reachable[i]?dist[i]:null);parents.add(parent[i]);}
        return obj("acyclic",true,"distance",distances,"parent_edge",parents);
    }
}
