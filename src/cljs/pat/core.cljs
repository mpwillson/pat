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
    (.fillText ctx "Hello, (idiot) World!" 100 100)
    (js/alert (str "<p>" colour "</p>"))))

(.write js/document "<p>Hello, clojurescript world!</p>")

