#!/usr/bin/python

import sys
import csv

with open(sys.argv[1], 'r') as fp:
  reader = csv.DictReader(fp)
#{
#    "count": "2",
#    "duration_unit": "milliseconds",
#    "m15_rate": "0.000000",
#    "m1_rate": "0.000000",
#    "m5_rate": "0.000000",
#    "max": "7.156000",
#    "mean": "4.330243",
#    "mean_rate": "0.556784",
#    "min": "1.588000",
#    "p50": "1.588000",
#    "p75": "7.156000",
#    "p95": "7.156000",
#    "p98": "7.156000",
#    "p99": "7.156000",
#    "p999": "7.156000",
#    "rate_unit": "calls/second",
#    "stddev": "2.783687",
#    "t": "1411240746"
#}
  for row in reader:
    print "%10s = %8s" % ("call count", row['count'])
    for key in ['mean_rate', 'm1_rate', 'm5_rate', 'm15_rate']:
      print "%10s = %8.2f calls/second" % (key, float(row[key]))
    for key in ['min', 'max', 'mean', 'stddev', 'p75', 'p95', 'p98', 'p99', 'p999']:
      print "%9s <= %8.2f milliseconds" % (key, float(row[key]))
    #for key in 
    print "\n"
