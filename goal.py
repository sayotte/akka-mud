
'''
    May have a parent goal
        If it's a top-level goal for an entity, it won't, otherwise it will
    May or may not have a strategy
        Strategy is used to set and order sub-goals
    If no strategy, must have an execution plan
        Execution plan is a FSM used to generate actions
    
    Meant to be subclassed, e.g.:
        MoveToStationaryTarget
            subscribe to existence of target
            haste?
        MoveToMovingTarget
            subscribe to existence and movement of target
            haste?
        AttackTarget
            subscribe to existence and "aliveness" of target
            aggression?
        ObtainInanimateEntity
            subscribe to existence and movement of target
        
'''

class Goal(Object):
        name = ''
        subgoals = []
        parent_goal = None
        strategy = None
        environment = {}

        def __init__(self):
            pass
 
        def strategize(self):
                ordered_goals = self.strategy.create_goals(self.environment)
                del self.subgoals
                self.subgoals = ordered_goals
 
        def select_strategy(self):
                ???
 
        def get_next_action(self):
                if(len(self.goals) > 0):
                        return self.goals[0].get_next_action
