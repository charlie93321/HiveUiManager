package com.hbgj.entity;

import java.util.*;

public class PairList<T,E> {
    private List<T> ts=new ArrayList<T>();
    private List<E> es=new ArrayList<E>();
    private Map<T,Integer> map=new HashMap<T,Integer>();

    public void add(T t,E e){
        map.put(t,ts.size());
        ts.add(t);
        es.add(e);
    }

    public List<E> sort(Comparator<T> comparator){
          ts.sort(comparator);
        List<E>   es2=new ArrayList<E>(es.size());
        for (int i = 0; i <ts.size() ; i++) {
            Integer ori=map.get(ts.get(i));
            es2.add(es.get(ori));
        }
        return es2;
    }

}
