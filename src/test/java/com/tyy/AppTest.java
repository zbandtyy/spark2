package com.tyy;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.junit.Test;
import org.opencv.core.Core;
import scala.Tuple2;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    /*编辑距离*/
    public   long levenshtein_distance(char s1[], char s2[]) {
        int len1 = s1.length, len2 = s2.length;
        int[] col = new int[len2 + 1];
        int[] prevCol = new int[len2 + 1];
        for ( int i = 0; i < prevCol.length; i++) prevCol[i]  = i;//第一行
        for (int i = 0; i < len1; i++) {
            col[0] = i + 1;//当前行
            for ( int j = 0; j < len2; j++) {
                col[j + 1] = Math.min(
                        Math.min(prevCol[1 + j] + 1, col[j] + 1),
                        prevCol[j] + (s1[i] == s2[j] ? 0 : 1));
            }
            int[] temp = prevCol;
            prevCol = col;
            col = temp;

        }
        return prevCol[len2];
    }
    @Test
    public void testDistance(){
        String a = "湘AHX0885";
        String b = "湘HX0885";
      //  String b = "湘MX0885";
//        char[] s = a.toCharArray();
//        for (int i = 0; i < s.length; i++) {
//            System.out.println(s[i]);
//        }
        long l = levenshtein_distance(a.toCharArray(), b.toCharArray());
        System.out.println(l);

    }
    @Setter @Getter @ToString
    class User{
        Integer id;
        String name;
        Integer Age;

        public User(int id, String a, int n) {
            this.id = id;
            this.name = a;
            this.Age = n;
        }
    }
    @Test
    public  void testJDK8Stream(){
        List<User> list = Arrays.asList(
                new User(1, "a", 10),
                new User(4, "a", 19),
                new User(5, "b", 13),
                new User(2, "b", 14),
                new User(3, "a", 10),
                new User(6, "f", 16));

        Optional<Integer> sum = list.stream().map(User::getAge).reduce(Integer::sum);
        Optional<Integer> max = list.stream().map(User::getAge).reduce(Integer::max);
        Optional<Integer> min = list.stream().map(User::getAge).reduce(Integer::min);
        System.out.println(sum);
        //测试的reduce只能返回一个值，做一种操作
        Optional<Tuple2<String, Integer>> reduce = list.stream().map((o) -> new Tuple2<String, Integer>(o.getName(), 1)).reduce(new BinaryOperator<Tuple2<String, Integer>>() {
            @Override
            public Tuple2<String, Integer> apply(Tuple2<String, Integer> stringIntegerTuple2, Tuple2<String, Integer> stringIntegerTuple22) {
                if (stringIntegerTuple2._1.equals(stringIntegerTuple22._1)) {
                    return new Tuple2<>(stringIntegerTuple2._1, stringIntegerTuple2._2 + stringIntegerTuple22._2);
                } else {
                    return stringIntegerTuple22;
                }
            }
        });
        System.out.println(reduce);
        Map<String, Long> collect = list.stream()
                .collect(Collectors.groupingBy(o -> o.getName(), Collectors.counting()));

        Map<String, Long> stringLongMap = collect.entrySet().stream().sorted(
                Comparator.comparing(o -> o.getValue())).map(entry -> {
            Map<String, Long> result = new LinkedHashMap<>();
            result.put(entry.getKey(), entry.getValue());
            return result;
        }).reduce((map1, map2) -> {
            map2.forEach(map1::put);
            return map1;
        }).get();
        System.out.println( stringLongMap.keySet().iterator().next());
        System.out.println( stringLongMap.keySet().iterator().next());

        System.out.println(collect);
        System.out.println(stringLongMap);




    }

    @Test
    public void testResize(){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);


    }
}
