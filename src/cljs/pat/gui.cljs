(ns gui)

(def graph-data (atom nil))
(def widget-map (atom {}))
(def refresh-fn (atom nil))
(def slot-vec (atom nil))

(def stride 20)

;;             green     red       blue
(def colorvec ["#88FF88" "#FF8888" "#6666FF"])

(defn canvas-available 
  []
  (.-CanvasRenderingContext2D js/window))

(defn init-slots [value]
  (reset! slot-vec value))

(defn get-value [field]
  (.-value (.getElementById js/document (name field))))

(defn set-value [field value]
  (set! (.-value (.getElementById js/document (name field))) value))

(defn get-checked [field]
  (.-checked (.getElementById js/document (name field))))

(defn get-canvas []
  (.getElementById js/document "patCanvas"))

(defn get-context []
  (.getContext (get-canvas) "2d"))

(defn get-int [field]
  (try
    (js/parseInt (get-value field))
    (catch js/Error _
      0)))

(defn colour-map
  [v sla]
  (cond
   (> v sla) 1
   (neg? v) 2
   (<= v sla) 0))
   
;; drawing many circles consumes much CPU time
(defn draw-symbol 
  [ctx x y radius colour]
  (set! (.-fillStyle ctx) colour)
  (.fillRect ctx x y radius radius))

(defn draw-column [ctx n cvals ew eh stride height sla]
  (doall
   (map (fn [v y]
          (draw-symbol ctx (* (inc n) stride) y 
                       ew (nth colorvec (colour-map v sla))))
        cvals (range (- height (* 2 stride)) 0 (* -1 stride)))))

(defn merge-slots
  "Remove adjacent slots with the same slot count."
  [sv nsv]
  (let [cur (first sv)
        nxt (second sv)]
    (cond
     (empty? sv) nsv
     (= (second cur) (second nxt))
     (recur (cons cur (rest (rest sv))) nsv)
     :else
     (recur (rest sv) (conj nsv cur)))))
     
(defn handle-slots
  [new-slot]
  ;; if entry for period already exists, remove it
  (let [slots (vec (filter #(not= (first %) (first new-slot)) @slot-vec))]
    (reset! slot-vec (merge-slots (vec (sort #(< (first %1) (first %2))
                                (into slots [new-slot]))) []))))

(defn draw-axes
  [ctx h nperiods stride]
  (set! (.-fillStyle ctx) "#00000")
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

(defn clear []
  (let [canvas (get-canvas)
        width (.-width canvas)
        height (.-height canvas)
        ctx (get-context)]
    (.clearRect ctx 0 0 width height)))

(defn draw-graph [values]
  (clear)
  (let [height (* (+ (get-int :nslots) 2) stride)
        sla (get-int :sla)
        nperiods (get-int :nperiods)
        ctx (get-context)
        marker-size (int (/ stride 2))]
    (set! (.-height (get-canvas)) height)
    (set! (.-width (get-canvas)) (* (+ nperiods 2) stride))
    (draw-axes ctx height nperiods stride)
    (doseq [n (range nperiods)]
      (draw-column ctx n (nth values n) marker-size marker-size
                   stride height sla))))

;; works for Chrome - needs testing for other browsers.
(defn event-coords 
  [event]
  (let [x (.-offsetX event)
        y (.-offsetY event)]
    (if (and x y)
      [x y]
      [(.-layerX event) (.-layerY event)])))

(defn mouse-click 
  [event]
  (let [[mouse-x  mouse-y] (event-coords event)
        height (.-height (get-canvas))
        period (dec (int (/ mouse-x stride)))
        x (* stride period)
        slots (int (/ (- height mouse-y) stride))
        y (* stride (int (/ mouse-y stride)))]
    (handle-slots [period slots])
    (when @refresh-fn (@refresh-fn))
    #_(set-value :status
               (format "Period: %d set with %d slots" period slots))))

(defn set-mouse-refresh [fn]
  (reset! refresh-fn fn))