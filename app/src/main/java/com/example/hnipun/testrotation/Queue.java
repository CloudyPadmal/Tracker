package com.example.hnipun.testrotation;

/**
 * Created by Sanj on 2/24/2017.
 */
public class Queue {

    private int total;

    private Node first, last;

    private class Node {
        private double ele;
        private Node next;
    }

    public Queue() { }

    public Queue enqueue(double ele)
    {
        Node current = last;
        last = new Node();
        last.ele = ele;

        if (total++ == 0) first = last;
        else current.next = last;

        return this;
    }

    public double dequeue()
    {
        if (total == 0) throw new java.util.NoSuchElementException();
        double ele = first.ele;
        first = first.next;
        if (--total == 0) last = null;
        return ele;
    }


    public double mean()
    {
        double meanValue=0;
        double sum =0;
        int count =0;
        Node tmp = first;
        while (tmp != null) {
            count++;
            sum += tmp.ele;
            tmp = tmp.next;
        }
        meanValue=sum/count;
        return meanValue;
    }



}
