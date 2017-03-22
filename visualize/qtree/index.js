'use strict';
// Top left: [40,9, -74.25]
// Bottom right: [40.5, -73.7]


let quadtree;

window.initMap = function() {
    d3.json("qtree.json", function (error, data) {
        if (error) throw error;
        var el = document.querySelector('#map');
        var google = window.google;
        function SVGOverlay (map) {
            this.map = map;
            this.svg = null;
            this.qt = null;
            this.points = [];

            this.onPan = this.onPan.bind(this);

            this.setMap(map);
        }

        SVGOverlay.prototype = new google.maps.OverlayView();

        SVGOverlay.prototype.onAdd = function () {
            this.svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            this.svg.style.position = 'absolute';
            this.svg.style.top = 0;
            this.svg.style.left = 0;
            this.svg.style.width = '100%';
            this.svg.style.height = '100%';
            this.svg.style.pointerEvents = 'none';

            const bounds = this.map.getBounds(),
                center = bounds.getCenter();

            const proj = this.getProjection();
            let pt_arr = [];
            $.each(data, function (i, json_obj) {
                pt_arr.push({x: json_obj.lat, y: json_obj.long});
            });
            this.points = pt_arr;

            this.qt = generate_quadtree_map(this.svg, this.points, proj);

            this.onPan();
            document.body.appendChild(this.svg);
            this.map.addListener('center_changed', this.onPan);
        };

        SVGOverlay.prototype.onPan = function () {
            console.log("pan");
            let proj = this.getProjection();
            redraw_qt(this.svg, this.qt, proj, this.points);
        };

        SVGOverlay.prototype.onRemove = function () {
            this.map.removeListener('center_changed', this.onPan);
            this.svg.parentNode.removeChild(this.svg);
            this.svg = null;
        };

        SVGOverlay.prototype.draw = function () {
            console.log('draw');
            let proj = this.getProjection();
            this.qt = generate_quadtree_map(this.svg, this.points, proj);
        };

        // Create the Google Map…
        const map = new google.maps.Map(d3.select("#map").node(), {
            zoom: 11,
            center: new google.maps.LatLng(40.7, -73.975),
            mapTypeId: google.maps.MapTypeId.TERRAIN
        });

        let overlay = new SVGOverlay(map);
    });
};

function transformXY(proj, x, y) {
    return proj.fromLatLngToContainerPixel(new google.maps.LatLng(x, y));
}

function generate_quadtree_map(svg, pt_arr, proj) {
    console.log(svg);

    quadtree = d3.geom.quadtree(pt_arr);

    const this_quadtree = nodes(quadtree);

    d3.select(svg)
        .selectAll("*").remove();

    let points = d3.select(svg)
        .selectAll(".point")
        .data(pt_arr);

    points.enter().append("circle")
        .attr("cx", (d) => transformXY(proj, d.x, d.y).x)
        .attr("cy", (d) => transformXY(proj, d.x, d.y).y)
        .attr("r", 2)
        .attr("opacity", 0.5)
        .attr("class", "point");

    let node = d3.select(svg)
        .selectAll(".node")
        .data(this_quadtree);

    let col = d3.scale.linear()
        .domain([0, quadtree.max_depth])
        .range(['green', 'red']);

    node.enter().append("rect")
        //
        .attr("x", function (d) {
            // return transformXY(proj, d.x + d.width, d.y).x;
            return transformXY(proj, d.x, d.y).x;
        })
        .attr("y", function (d) {
            // return transformXY(proj, d.x + d.width, d.y).y;
            return transformXY(proj, d.x2, d.y2).y;
        })
        .attr("width", function (d) {
            // let tl = transformXY(proj, d.x + d.width, d.y);
            // let br = transformXY(proj, d.x, d.y + d.height);

            let p1 = transformXY(proj, d.x, d.y);
            let p2 = transformXY(proj, d.x2, d.y2);

            return p2.x - p1.x;
        })
        .attr("height", function (d) {
            // let tl = transformXY(proj, d.x + d.width, d.y);
            // let br = transformXY(proj, d.x, d.y + d.height);
            let p1 = transformXY(proj, d.x, d.y);
            let p2 = transformXY(proj, d.x2, d.y2);
            return p1.y - p2.y;
        })
        .style("stroke-opacity", .5)
        .style("position", "absolute")
        .attr('fill-opacity', function (d) {
            return 0.5;
            // return (d.depth / quadtree.max_depth);
        })
        .style("fill", (d) => d.is_leaf ? col(d.depth) : "none")
        .attr("class", function (d) {
            if (!(d.pts == null) || d.is_leaf) {
                return "node full_node";
            }
            else {
                return "node empty_node";
            }
        });
    node.exit().remove();

    return this_quadtree;
}

function redraw_qt(svg, qt, proj, points) {
    let node = d3.select(svg)
        .selectAll(".node")
        .data(qt)
        .attr("x", function (d) {
            return transformXY(proj, d.x, d.y).x;
        })
        .attr("y", function (d) {
            return transformXY(proj, d.x2, d.y2).y;
        });

    d3.select(svg)
        .selectAll(".point")
        .data(points)
        .attr("cx", (d) => transformXY(proj, d.x, d.y).x)
        .attr("cy", (d) => transformXY(proj, d.x, d.y).y)
}

// Collapse the quadtree into an array of rectangles.
function nodes(quadtree) {
    const nodes = [];
    quadtree.depth = 0; // root depth
    let node_cnt = 0;

    quadtree.max_depth = 0;

    quadtree.visit(function (node, x1, y1, x2, y2) {
        node_cnt++;
        const np = node.point;
        for (let i=0; i<4; i++) {
            if (node.nodes[i]) node.nodes[i].depth = node.depth+1;
        }
        if (np) {
            console.log(np);
        }
        nodes.push({
            x: x1,
            y: y1,
            x2: x2,
            y2: y2,
            // width: x2 - x1,
            // height: y2 - y1,
            // area: ((y2 - y1) * (x2 - x1)),
            pts: np,
            node_no: node_cnt,
            is_leaf: node.leaf,
            depth: node.depth
        });

        if (node.depth > quadtree.max_depth) {
            quadtree.max_depth = node.depth;
        }
    });
    return nodes;
}