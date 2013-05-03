(ns gui)


(defn get-value [field]
  (.-value (.getElementById js/document (name field))))

(defn get-context []
  (-> js/document  (.getElementById "myCanvas") (.getContext "2d")))

(defn draw-axes
  [ctx h nperiods stride]
  (set! (.-fillStyle "000000"))
  (doseq [n (range nperiods)]
    (.fillText ctx (str n) (* (inc n) stride) (- h (/ stride 2))))
  (doall (map
         #(.fillText ctx (format "%2d" %2) 1 %1)
         (range (- h stride 10) 0 (- stride))
         (range 1 (inc nperiods))))
  ;; draw markers
  (doseq [x (range (* 1 stride) (* (inc nperiods) stride) stride)
          y (range (- h (+ 10 stride)) 0 (- stride))]
    (.fillText ctx "+" x y)))

(defn draw-graph []
  (js/alert (get-value :colour))
  (draw-axes (get-context) 1000 52 20))
