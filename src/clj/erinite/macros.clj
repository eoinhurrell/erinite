(ns erinite.macros)

(defmacro configure
  [name-sym & options]
  (let [name (str name-sym)]
    `(def ~name-sym (erinite.core/configure-app
                 ~name
                 (hash-map ~@options)))))
