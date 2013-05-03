
;;;; simul - simple simulation of queuing for service
;;;;

(ns simul
  (:use clojure.repl)
  (:import (java.util Random)))

;; service request and completion rates per period
(def sar (repeat 22)) ; service request arrival rate
(def scr (repeat 19)) ; service completion rate

(def span (atom (vector)))


(defn grand
  "Return random integer with normal distribution around the mean."
  [mean]
  (let [gen (Random.)
        u1 (.nextDouble gen)
        u2 (.nextDouble gen)
        n (* (Math/sqrt (* -2 (Math/log u1))) (Math/cos (* 2 Math/PI u2)))]
    (int (+ (* 8.333 n) mean))))

(defn mkqueue []
     (atom clojure.lang.PersistentQueue/EMPTY))

(defn enqueue [q elt]
  (swap! q conj elt)
  q)

(defn peek-queue [q]
     (peek @q))

(defn dequeue [q]
  (if-let [head (peek-queue q)]
    (do (swap! q pop)
        head)
    nil))

(defn add-period [v elt]
  (swap! v conj elt))

(defn vref [v n]
  (nth @v n))

(defn request [period])

(defn add-requests
  [q period nreqs]
  (if (pos? nreqs)
    (recur (enqueue q period) period (dec nreqs))
    q))

(defn gen-periods
  [p period-defn]
  (let [current (first period-defn)
        next (next period-defn)]
    (if next
      (recur (concat p (repeat (- (ffirst next) (first current))
                               (second current))) next)
      (concat p (repeat (second current))))))

(defn service-reqs
  [q current nserved]
  #_(println (count (seq @q)) current nserved)
  (doall (for [n (range nserved)]
           (let [started (dequeue q)]
             (if started
               (- current started)
               -1)))))

(defn run-simul
  [q nperiods service-rates arrival-rate-fn]
  (for [n (range nperiods)]
    (do (add-requests q n (arrival-rate-fn))
        (service-reqs q n (nth service-rates n)))))
