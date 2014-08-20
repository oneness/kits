(ns kits.queues
  "Wrappers for constructing various java.util.concurrent queues."
  (:require [kits.thread :as thread])
  (:import (java.util.concurrent ArrayBlockingQueue BlockingQueue
                                 PriorityBlockingQueue TimeUnit))
  (:refer-clojure :exclude [get peek empty?]))

(set! *warn-on-reflection* true)

(defn make-basic-queue
  "Create a new queue that can hold at max 'capacity' items"
  [& [capacity]]
  (let [capacity (or capacity 10)]
    (ArrayBlockingQueue. (int capacity))))

(def create make-basic-queue)           ; compatibility alias

(defn make-priority-queue
  "Create a new priority-queue with `comparator` and `capacity` items"
  [comparator & [capacity]]
  (let [capacity (or capacity 10)]
    (PriorityBlockingQueue. capacity comparator)))

(defn add
  "Add a new msg to the queue. Returns false if the msg could not be
   added because the queue is full, true otherwise."
  [^BlockingQueue q msg]
  (.offer q msg))

(def offer! add)                        ; compatibility alias

(defn fetch
  "Retrieves a message from the queue, waiting if necessary until an
   element becomes available."
  ([^BlockingQueue q]
     (fetch q 0))
  ([^BlockingQueue q timeout-in-ms]
     (.poll q timeout-in-ms TimeUnit/MILLISECONDS)))

(def poll! fetch)                       ; compatibility alias

(defn fetch-with-timeout
  "Retrieves a message from the queue, waiting if necessary until an
   element becomes available."
  [^BlockingQueue q timeout-in-ms]
  (.poll q timeout-in-ms TimeUnit/MILLISECONDS))

(defn peek
  "Retrieves, but does not remove, a message from the queue"
  [^BlockingQueue q]
  (.peek q))

(defn used [^BlockingQueue q]
  (.size q))

(defn empty? [q]
  (== 0 (used q)))

(defn free [^BlockingQueue q]
  (.remainingCapacity q))

(defn stats
  "Return current stats for the queue"
  [^BlockingQueue q]
  (let [s (used q)
        r (free q)]
    {:total (+ s r)
     :used s
     :free r}))
