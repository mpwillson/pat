(ns gui
  (:require [cljs.reader :as reader]
            [goog.string :as gstring] ; both this and next line required for
            [goog.string.format]))    ; goog.string.format

(def graph-data (atom nil))
(def widget-map (atom {}))
(def refresh-fn (atom nil))
(def slot-vec (atom nil))
(def file-read-fn (atom nil))

(def stride 20)

;;             green     red       blue
(def colorvec ["#88FF88" "#FF8888" "#6666FF"])


(defn format 
  "Emulate clojure format via goog.string.format."
  [fmt & args]
  (apply gstring/format fmt args))

(defn canvas-available 
  []
  (.-CanvasRenderingContext2D js/window))

(defn init-slots [value]
  (reset! slot-vec value))

(defn get-element [id]
  (.getElementById js/document (name id)))

(defn get-value [id]
  (reader/read-string (.-value (get-element id))))

(defn set-value [id value]
  (set! (.-value (get-element id)) value))

(defn get-checked [id]
  (.-checked (get-element id)))

(defn set-checked [id value]
  (set! (.-checked (get-element id)) value))

(defn get-context [canvas]
  (.getContext canvas "2d"))

(defn colour-map
  [v sla]
  (cond
   (> v sla) 1
   (neg? v) 2
   (<= v sla) 0))
   
;; drawing many circles consumes much CPU time
(defn draw-symbol 
  [ctx x y size colour]
  (set! (.-fillStyle ctx) colour)
  (.fillRect ctx x y size size))

(defn draw-column [ctx n cvals ew eh stride height sla]
  (doall
   (map (fn [v y]
          (draw-symbol ctx (* (inc n) stride) y 
                       ew (nth colorvec (colour-map v sla))))
        cvals (range (- height (* 2.5 stride)) 0 (- stride)))))

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
  [ctx h nslots nperiods stride]
  (set! (.-fillStyle ctx) "#00000")
  (doseq [n (range nperiods)]
    (.fillText ctx (str n) (* (inc n) stride) (- h stride 2)))
  (doall (map
         #(.fillText ctx (format "%02d" %2) 1 %1)
         (range (- h (* 2 stride)) 0 (- stride))
         (range 1 (inc nslots))))
  ;; draw markers
  (doseq [x (range (* 1 stride) (* (inc nperiods) stride) stride)
          y (range (- h (* 2 stride)) 0 (- stride))]
    (.fillText ctx "+" x y)))

(defn clear []
  (let [canvas (get-element :patCanvas)
        width (.-width canvas)
        height (.-height canvas)
        ctx (get-context canvas)]
    (.clearRect ctx 0 0 width height)))

(defn draw-graph [values]
  (clear)
  (let [canvas (get-element :patCanvas)
        ctx (get-context canvas)
        nslots (js/parseInt (get-value :nslots))
        height (* (+ nslots 2) stride)
        sla (get-value :sla)
        nperiods (js/parseInt (get-value :nperiods))
        marker-size (int (/ stride 2))]
    (set! (.-height canvas) height)
    (set! (.-width canvas) (* (+ nperiods 2) stride))
    (draw-axes ctx height nslots nperiods stride)
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
        height (.-height (get-element :patCanvas))
        period (dec (int (/ mouse-x stride)))
        x (* stride period)
        slots (dec (int (/ (- height mouse-y) stride)))
        y (* stride (int (/ mouse-y stride)))]
    (handle-slots [period slots])
    (when @refresh-fn (@refresh-fn))))

(defn set-mouse-refresh [fn]
  (reset! refresh-fn fn))

(defn handle-file-contents [text]
  (when @file-read-fn (@file-read-fn text)))

(defn read-file [evt]
  (let [f (aget (.-files (.-target evt)) 0)]
    (if f
      (let [r (js/FileReader.)]
        (set! (.-onload r) 
              (fn [e] (handle-file-contents (.-result (.-target e)))))
        (.readAsText r f))
      (js/alert "Unable to read file"))))

(defn set-file-listener [callback]
  (reset! file-read-fn callback)
  (let [filer (get-element :filer)]
    (.addEventListener filer "change" read-file false)))