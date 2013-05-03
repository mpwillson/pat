;;;; GUI for pat.clj
;;;;
;;;; Invoke initialise using
;;;;  (. javax.swing.SwingUtilities (invokeAndWait gui/initialise))
;;;; to ensure swing gui creation occurs on the Event Dispatching Thread (EDT).

(ns gui 
  (:import (javax.swing JFrame JLabel JTextField JButton JTextArea 
                        JScrollPane JPanel BoxLayout Box JTable
                        BorderFactory JRadioButton ButtonGroup
                        JOptionPane)
           (javax.swing.border BevelBorder)
           (java.awt.event ActionListener MouseListener)
           (java.awt GridLayout Dimension Panel Graphics2D Color Font
                     FlowLayout GridBagLayout GridBagConstraints)))

(def graph-data (atom nil))
(def widget-map (atom {}))
(def refresh-fn (atom nil))
(def slot-vec (atom [[0 22]]))

(def colorvec [Color/green Color/red Color/blue])

;; macro to define button press actions
(defmacro callback [widget & body]
    `(. ~widget
       (addActionListener
        (proxy [ActionListener] []
          (actionPerformed [evt#]
                           ~@body)))))

(defmacro do-edt 
  "Invoke body on Swing event dispatching thread. Returns immediately."
  [& body]
  `(javax.swing.SwingUtilities/invokeLater (fn [] ~@body)))

(defn set-gb-constraint
  [panel widget
   & {:keys [gridx gridy weightx gridwidth gridheight fill ipadx ipady]
      :or {gridx 0 gridy 0 weightx 0.5 gridwidth 1
           gridheight 1 fill GridBagConstraints/HORIZONTAL
           ipadx 2 ipady 2}}]
    (.add panel widget
          (doto (GridBagConstraints.)
            (-> .gridx (set! gridx))
            (-> .gridy (set! gridy))
            (-> .weightx (set! weightx))
            (-> .gridwidth (set! gridwidth))
            (-> .gridheight (set! gridheight))
            (-> .fill (set! fill))
            (-> .ipadx (set! ipadx))
            (-> .ipady (set! ipady)))))

(defn get-field
  [id]
  (if-let [widget (id @widget-map)]
    (-> widget .getLabelFor .getText)
    ""))

(defn set-field
  [id value]
  (if-let [widget (id @widget-map)]
    (-> widget .getLabelFor (.setText value))
    ""))

(defn make-field
  [id label-text]
  (let [label (JLabel. label-text JLabel/TRAILING)
        field (JTextField.)]
    (.setLabelFor label field)
    (swap! widget-map into {id label})))

(defn add-field
  [panel id x y]
  (let [c (GridBagConstraints.)
        label (id @widget-map)]
    (when label
      (set-gb-constraint panel label :gridx x :gridy y :weightx 0.01)
      (set-gb-constraint panel (.getLabelFor label) :gridx (inc x) :gridy y
                         :weightx 0.9))))
      
(defn get-int
  [field]
  (try
   (Integer. (gui/get-field field))
   (catch NumberFormatException _ 0)))

(defn make-rbutton
  [id text]
  (let [rb (JRadioButton. text)]
    (swap! widget-map into {id rb})
    rb))

(defn make-buttonbox [rbuttons]
  (let [bg (ButtonGroup.)]
    (doseq [rb rbuttons] (.add bg (rb @widget-map)))
    bg))

(defn set-rbutton
  [id]
  (.setSelected (id @widget-map) true))

(defn get-rbutton
  [id]
  (.isSelected (id @widget-map)))

(defn set-border
  [panel title]
  (.setBorder panel (BorderFactory/createCompoundBorder 
                     (BorderFactory/createEmptyBorder 5 5 5 5) 
                     (BorderFactory/createTitledBorder
                      (BevelBorder. BevelBorder/LOWERED)
                      title))))

(defn colour-map
  [v sla]
  (cond
   (> v sla) 1
   (neg? v) 2
   (<= v sla) 0))
   
(defn draw-column [g n cvals ew eh stride height sla]
  (doall
   (map (fn [v y]
          (.setColor g (nth colorvec (colour-map v sla)))
          (.fillOval g (* (inc n) stride) y ew eh))
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
  [g h nperiods stride]
  (.setColor g Color/black)
  (doseq [n (range nperiods)]
    (.drawString g (str n) (* (inc n) stride) (- h (/ stride 2))))
  (doall (map
         #(.drawString g (format "%2d" %2) 1 %1)
         (range (- h stride 10) 0 (- stride))
         (range 1 (inc nperiods))))
  ;; draw markers
  (doseq [x (range (* 1 stride) (* (inc nperiods) stride) stride)
          y (range (- h (+ 10 stride)) 0 (- stride))]
    (.drawString g "+" x y)))

(defn make-graph []
  (proxy [JPanel MouseListener] []
              (paintComponent
               [g]
               (let [h (-> this .getSize .height)
                     nperiods (count @graph-data)
                     sla (get-int :sla)
                     stride 20]
                 (proxy-super paintComponent g)
                 (draw-axes g h (get-int :nperiods) stride)
                 (doseq [n (range nperiods)]
                   (draw-column g n (nth @graph-data n) 10 10 stride h sla))))
              (mouseClicked
               [e]
               (let [g (.getGraphics this)
                     h (-> this .getSize .height)
                     period (dec (int (/ (.getX e) 20)))
                     x (* 20 period)
                     slots (int (/ (- h (.getY e)) 20))
                     y (* 20 (int (/ (.getY e) 20)))]
                 (handle-slots [period slots])
                 (when @refresh-fn (@refresh-fn))
                 (set-field :status
                  (format "Period: %d set with %d slots" period slots))))
              (mousePressed [e])
              (mouseEntered [e])
              (mouseExited [e])
              (mouseReleased [e])))

;; GUI management
(let [frame (JFrame.)
      graph (make-graph)
      scroll (JScrollPane. graph)
      dp (JPanel. (GridBagLayout.))
      cp (JPanel.)
      sp (JPanel. (GridBagLayout.))
      rbp (JPanel. (GridLayout. 2 1))
      pp (JPanel. (GridBagLayout.))
      draw-button (JButton. "Show Slots")
      run-button (JButton. "Run")
      clear-button (JButton. "Clear")
      quit-button (JButton. "Quit")
      button-map {:draw draw-button :run run-button 
                  :quit quit-button :clear clear-button}]

  (defn initialise
    [title]
    ;; setup data panel
    (doto dp
      (.setMaximumSize (Dimension. 615 100))
      (.setMinimumSize (Dimension. 615 100))
      (set-border "Parameters"))
    (doseq [[k lt x y] [[:nperiods "# Periods: " 0 0]
                      [:nreferrals "# Referrals/Period: " 0 1]
                      [:nqueued "# Patients Queued: " 0 2]
                      [:nslots "# Max Slots/Period: " 2 0]
                      [:sla "Breech Threshold: " 2 1]]]
      (make-field k lt)
      (add-field dp k x y))

    ;; setup status panel
    (doto sp
      (.setBorder (BorderFactory/createEmptyBorder 5 0 5 0))
      (.setMaximumSize (Dimension. 415 50))
      (.setMinimumSize (Dimension. 415 50)))
    (make-field :status "Status: ")
    (add-field sp :status 0 0)

    (doto graph
      (.setPreferredSize (Dimension. 1000 750))
      (.setBackground Color/white)
      (.addMouseListener graph))
    (set-border scroll "Simulation Results")

    ;; setup button box
    (doto cp
      (.setLayout (new GridLayout 1 4 5 5))
      (.setMaximumSize (Dimension. 415 50))
      (.setMinimumSize (Dimension. 415 50))
      (.setBorder (BorderFactory/createEmptyBorder 5 0 5 0))
      (.add draw-button)
      (.add run-button)
      (.add clear-button)
      (.add quit-button))
    ;; setup frame
    (.setLayout (.getContentPane frame) 
                (BoxLayout. (.getContentPane frame) BoxLayout/Y_AXIS))
    
    ;; setup radio buttons
    (.add rbp (make-rbutton :uniform "Uniform"))
    (.add rbp (make-rbutton :normal "Normal"))
    (make-buttonbox [:uniform :normal])
    (set-rbutton :uniform)

    (doto rbp
      (set-border "Referral Distribution")
      (.setMinimumSize (Dimension. 200 200))
      (.setMaximumSize (Dimension. 200 200)))

    (doto pp
      (.setMinimumSize (Dimension. 1000 200))
      (.setMaximumSize (Dimension. 1000 200))
      (set-gb-constraint dp :gridx 0 :gridy 0 :gridheight 2)
      (set-gb-constraint rbp :gridx 1 :gridy 0))
      ;;(.add dp)
      ;;(.add rbp))
    
    (doto frame
        (.setTitle title)
        (.add pp)
        (.add scroll)
        (.add sp)
        (.add cp)
        (.setSize 1000 900)
        (.setVisible true)))

  (defn start [title]
     (initialise title))

  (defn stop []
    (.dispose frame))

  (defn draw-graph [values]
    (let [max-height (get-int :nslots)]
      (.setPreferredSize
       graph (Dimension. (* (count values) 20) (* max-height 20)))
      (reset! graph-data values)
      (.repaint graph)
      (.revalidate graph)))

  (defn draw-slots
    []
    (reset! graph-data nil)
    (.repaint graph)
    (.revalidate graph))

  (defn set-mouse-refresh 
    [f]
    (reset! refresh-fn f))

  (defn clear []
    (let [g (.getGraphics graph)
          w (-> graph .getSize .width)
          h (-> graph .getSize .height)]
      (.setColor g Color/white)
      (.fillRect g 0 0 w h)))

 (defn set-status [status]
    (set-field :status status))
 (defn get-status []
   (get-field :status))

  (defn set-title [title]
    (.setTitle frame title))


(defn messagebox
  [title message]
  (JOptionPane/showMessageDialog frame message title JOptionPane/PLAIN_MESSAGE))

(defn clear-action-listeners[button]
    (let [als (.getActionListeners button)]
      (doseq [al als]
        (.removeActionListener button al))))

  (defn set-callback [b callback]
    (let [button (get button-map b)]
      (if button
        (do  
          (clear-action-listeners button) 
          (.addActionListener button
                             (proxy [ActionListener] []
                               (actionPerformed [evt]
                                                (callback)))))))))

