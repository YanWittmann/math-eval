# Plots demo

```result=null;;;(r ) -> { p.x = []; p.y = []; for (i : space(0, 2 * pi, 360)) { p.x = p.x :: (r * cos(i)); p.y = p.y :: (r * sin(i)); }; return p; };;;{x: [10, 9.998476951563913, 9.993908270190958, 9.986295347545738, 9.975640502598242, 9.961946980917455, 9.945218953682733, 9.9254615164132
import cmdplot;;;generate_points = r -> {
    p.x = []
    p.y = []
    for (i in space(0, 2 * PI, 360)) {
        p.x = p.x :: (r * cos(i))
        p.y = p.y :: (r * sin(i))
    }
    return p
};;;points = generate_points(10);;;cmdplot.plot(62, 25, points.x, points.y)
```
