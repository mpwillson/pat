(ns pat.core)

(defn fill []
  (let [c (.getElementById js/document "myCanvas")
        width (.-width c)
        height (.-height c)
        ctx (.getContext c "2d")
        colour (.-value (.getElementById js/document "colour"))]
    (if (= colour "")
      (set! (.-fillStyle ctx) "#FF9100")
      (set! (.-fillStyle ctx) colour))
    (.clearRect ctx 0 0 width height)
    (.fillRect ctx 0 0 width height)
    (set! (.-fillStyle ctx) "#000000")
    (doseq [x (range 0 width 20) y (range 0 height 20)]
      (.fillText ctx "+" x y))
    (js/alert (str "<p>" colour "</p>"))))

(.write js/document "<p>Hello, clojurescript world!</p>")

